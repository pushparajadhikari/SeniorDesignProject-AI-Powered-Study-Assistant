"""
AI Study Assistant — FastAPI Backend
=====================================
Wraps the RAG pipeline (ChromaDB + Ollama) and exposes REST endpoints
consumed by the Android app.

Run:
    pip install -r requirements.txt
    uvicorn main:app --host 0.0.0.0 --port 8000 --reload
"""

from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import uvicorn
import os
import json
import re
import hashlib
import base64
import requests
import fitz          # PyMuPDF
import chromadb

# ── CONFIG ─────────────────────────────────────────────────────────────────────
# Ollama is expected to run on the same machine as this backend.
# If it's on a different host, change OLLAMA_BASE_URL here.
OLLAMA_BASE_URL = "http://localhost:11434"

LLM_MODEL    = "llama3.2:1b"    # text generation model
EMBED_MODEL  = "nomic-embed-text"   # embedding model
VISION_MODEL = "moondream"          # image description model

TOP_K_CHUNKS  = 5
CHUNK_SIZE    = 500
CHUNK_OVERLAP = 50

CHROMA_DIR = "./chroma_db"
HASH_FILE  = "./indexed_files.json"
# ──────────────────────────────────────────────────────────────────────────────

app = FastAPI(title="AI Study Assistant Backend", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ── ChromaDB helpers ──────────────────────────────────────────────────────────

def get_collection():
    """Return (or create) the persistent ChromaDB collection."""
    client = chromadb.PersistentClient(path=CHROMA_DIR)
    try:
        return client.get_collection("study_notes")
    except Exception:
        return client.create_collection(
            name="study_notes",
            metadata={"hnsw:space": "cosine"}
        )


# ── Hash file helpers ─────────────────────────────────────────────────────────

def load_indexed_hashes() -> dict:
    if os.path.exists(HASH_FILE):
        with open(HASH_FILE, "r") as f:
            return json.load(f)
    return {}


def save_indexed_hashes(hashes: dict):
    with open(HASH_FILE, "w") as f:
        json.dump(hashes, f, indent=2)


def get_bytes_hash(data: bytes) -> str:
    return hashlib.md5(data).hexdigest()


# ── Text chunking ─────────────────────────────────────────────────────────────

def chunk_text(text: str) -> list[str]:
    chunks = []
    start = 0
    while start < len(text):
        end = start + CHUNK_SIZE
        chunks.append(text[start:end])
        start += CHUNK_SIZE - CHUNK_OVERLAP
    return [c for c in chunks if c.strip()]


# ── Ollama wrappers ───────────────────────────────────────────────────────────

def get_embedding(text: str) -> list[float]:
    resp = requests.post(
        f"{OLLAMA_BASE_URL}/api/embeddings",
        json={"model": EMBED_MODEL, "prompt": text},
        timeout=30,
    )
    resp.raise_for_status()
    return resp.json()["embedding"]


def describe_image(img_b64: str) -> str:
    payload = {
        "model": VISION_MODEL,
        "messages": [{
            "role": "user",
            "content": (
                "Describe everything in this diagram in detail. "
                "Include all labels, numbers, names, arrows, colors, and any data shown."
            ),
            "images": [img_b64],
        }],
        "stream": False,
    }
    try:
        resp = requests.post(f"{OLLAMA_BASE_URL}/api/chat", json=payload, timeout=60)
        return resp.json()["message"]["content"]
    except Exception as e:
        return f"[Image description unavailable: {e}]"


def call_llm(prompt: str) -> str:
    """Send a prompt to the LLM and return the full response text (non-streaming)."""
    payload = {
        "model": LLM_MODEL,
        "messages": [{"role": "user", "content": prompt}],
        "stream": False,
    }
    resp = requests.post(f"{OLLAMA_BASE_URL}/api/chat", json=payload, timeout=120)
    resp.raise_for_status()
    return resp.json()["message"]["content"]


# ── PDF processing ────────────────────────────────────────────────────────────

def extract_page_data(pdf_bytes: bytes, source_name: str) -> list[dict]:
    doc = fitz.open(stream=pdf_bytes, filetype="pdf")
    pages = []
    for i, page in enumerate(doc):
        text = page.get_text().strip()
        images = []
        for img in page.get_images(full=True):
            xref = img[0]
            try:
                base_image = doc.extract_image(xref)
                img_b64 = base64.b64encode(base_image["image"]).decode("utf-8")
                images.append(img_b64)
            except Exception:
                pass
        pages.append({"text": text, "images": images, "source": source_name, "page": i + 1})
    return pages


def index_pdf_bytes(pdf_bytes: bytes, filename: str, collection) -> int:
    """Index all text chunks and image descriptions from a PDF. Returns chunk count added."""
    pages = extract_page_data(pdf_bytes, filename)
    existing_count = collection.count()
    chunk_id = existing_count

    all_texts, all_embeddings, all_ids, all_metadata = [], [], [], []

    for page_data in pages:
        page = page_data["page"]

        if page_data["text"]:
            for chunk in chunk_text(page_data["text"]):
                embedding = get_embedding(chunk)
                all_texts.append(chunk)
                all_embeddings.append(embedding)
                all_ids.append(f"chunk_{chunk_id}")
                all_metadata.append({"source": filename, "page": page, "type": "text"})
                chunk_id += 1

        for img_b64 in page_data["images"]:
            description = describe_image(img_b64)
            if description.strip():
                image_chunk = f"[Diagram on page {page} of {filename}]: {description}"
                embedding = get_embedding(image_chunk)
                all_texts.append(image_chunk)
                all_embeddings.append(embedding)
                all_ids.append(f"chunk_{chunk_id}")
                all_metadata.append({"source": filename, "page": page, "type": "image_description"})
                chunk_id += 1

    if all_texts:
        collection.add(
            documents=all_texts,
            embeddings=all_embeddings,
            ids=all_ids,
            metadatas=all_metadata,
        )

    return len(all_texts)


# ── RAG retrieval ─────────────────────────────────────────────────────────────

def retrieve(collection, question: str) -> tuple[str, list[str]]:
    """Retrieve the top-K most relevant chunks and their source citations."""
    count = collection.count()
    if count == 0:
        return "", []

    query_embedding = get_embedding(question)
    results = collection.query(
        query_embeddings=[query_embedding],
        n_results=min(TOP_K_CHUNKS, count),
    )
    chunks = results["documents"][0]
    metas  = results["metadatas"][0]

    context_parts = []
    sources = []
    for chunk, meta in zip(chunks, metas):
        label = "📷 Diagram" if meta["type"] == "image_description" else "📄 Text"
        context_parts.append(f"[{label} — {meta['source']}, Page {meta['page']}]\n{chunk}")
        src = f"{meta['source']} — Page {meta['page']}"
        if src not in sources:
            sources.append(src)

    return "\n\n---\n\n".join(context_parts), sources


# ── Endpoints ─────────────────────────────────────────────────────────────────

@app.get("/health")
async def health():
    """Heartbeat — used by the Android app's server-status indicator."""
    return {"status": "ok"}


@app.post("/upload")
async def upload_pdf(file: UploadFile = File(...)):
    """
    Accept a PDF upload, run the RAG indexing pipeline, and persist to ChromaDB.
    Skips re-indexing if the exact same file (by MD5) was already processed.
    """
    if not file.filename or not file.filename.lower().endswith(".pdf"):
        raise HTTPException(status_code=400, detail="Only PDF files are supported")

    pdf_bytes = await file.read()
    filename  = file.filename

    # De-duplicate by content hash
    indexed_hashes = load_indexed_hashes()
    current_hash   = get_bytes_hash(pdf_bytes)

    if indexed_hashes.get(filename) == current_hash:
        return {"success": True, "message": "Already indexed (no changes detected)", "filename": filename}

    collection = get_collection()
    try:
        chunk_count = index_pdf_bytes(pdf_bytes, filename, collection)
        indexed_hashes[filename] = current_hash
        save_indexed_hashes(indexed_hashes)
        return {
            "success":  True,
            "message":  f"Successfully indexed {filename} ({chunk_count} chunks)",
            "filename": filename,
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Indexing failed: {e}")


class ChatRequest(BaseModel):
    question: str


@app.post("/chat")
async def chat(req: ChatRequest):
    """
    Answer a student question using RAG:
    retrieve relevant chunks → build prompt → call LLM → return answer + sources.
    """
    if not req.question.strip():
        raise HTTPException(status_code=400, detail="Question cannot be empty")

    collection = get_collection()
    context, sources = retrieve(collection, req.question)

    if not context:
        return {
            "answer":  "No documents have been indexed yet. Please upload a PDF first.",
            "sources": [],
        }

    prompt = f"""You are a helpful study assistant. Answer the student's question using ONLY the information from the retrieved study material below. Be concise, accurate, and helpful. If the material doesn't contain enough information to answer the question, say so clearly.

=== RETRIEVED CONTENT ===
{context}

=== STUDENT QUESTION ===
{req.question}

Answer:"""

    try:
        answer = call_llm(prompt)
        return {"answer": answer.strip(), "sources": sources}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"LLM error: {e}")


@app.post("/quiz")
async def generate_quiz():
    """
    Generate 3 multiple-choice questions from indexed content.
    Returns structured JSON the Android app can render directly.
    """
    collection = get_collection()
    context, _ = retrieve(collection, "key facts important concepts definitions diagrams examples")

    if not context:
        raise HTTPException(
            status_code=400,
            detail="No documents indexed. Upload a PDF first."
        )

    prompt = f"""Based on the following study material, create exactly 3 multiple choice quiz questions to test a student's understanding.

IMPORTANT: Respond with ONLY valid JSON. No explanations, no markdown code fences, no extra text before or after the JSON.

Required format:
{{"questions": [{{"question": "Question text?", "options": ["Option A", "Option B", "Option C", "Option D"], "correct_index": 0, "explanation": "Brief explanation why this answer is correct"}}]}}

Rules:
- Each question must have exactly 4 options
- correct_index must be 0, 1, 2, or 3 (zero-based index of the correct option)
- Questions must be answerable from the study material below
- Do not add any text outside the JSON object

Study Material:
{context}

JSON:"""

    try:
        raw = call_llm(prompt)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"LLM error: {e}")

    # ── Parse LLM response robustly ───────────────────────────────────────────
    text = raw.strip()

    # Strip markdown code fences if the LLM wrapped the JSON
    text = re.sub(r'^```[\w]*\n?', '', text)
    text = re.sub(r'\n?```$',      '', text)
    text = text.strip()

    # Try direct parse first, then extract the first JSON object as fallback
    data = None
    try:
        data = json.loads(text)
    except json.JSONDecodeError:
        match = re.search(r'\{.*\}', text, re.DOTALL)
        if match:
            try:
                data = json.loads(match.group())
            except json.JSONDecodeError:
                pass

    if not data or "questions" not in data:
        raise HTTPException(
            status_code=500,
            detail="LLM did not return valid JSON. Try again."
        )

    # Normalise: ensure each question has 4 options and a valid correct_index
    for q in data["questions"]:
        opts = q.get("options", [])
        while len(opts) < 4:
            opts.append("N/A")
        q["options"] = opts[:4]
        if "correct_index" not in q or not isinstance(q["correct_index"], int):
            q["correct_index"] = 0
        q["correct_index"] = max(0, min(3, q["correct_index"]))
        if "explanation" not in q:
            q["explanation"] = ""

    return data


@app.get("/docs-list")
async def docs_list():
    """Return the filenames of all indexed documents."""
    hashes    = load_indexed_hashes()
    documents = list(hashes.keys())
    return {"documents": documents}


@app.delete("/docs/{filename}")
async def delete_doc(filename: str):
    """
    Remove a document from the ChromaDB index and the hash registry.
    Deletes all chunks whose metadata.source == filename.
    """
    collection = get_collection()
    try:
        results = collection.get(where={"source": filename})
        if results["ids"]:
            collection.delete(ids=results["ids"])
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to delete from index: {e}")

    hashes = load_indexed_hashes()
    if filename in hashes:
        del hashes[filename]
        save_indexed_hashes(hashes)

    return {"success": True, "message": f"Deleted '{filename}' from index"}


# ── Entry point ───────────────────────────────────────────────────────────────

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
