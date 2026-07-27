# StudyAI — AI-Powered Study Assistant

An Android application that helps students study from their own course material. Upload your
lecture PDFs, ask questions and get answers drawn **only** from your notes, generate practice
quizzes and flashcards from a chosen document, and track your progress across sessions. Because
answers are grounded in your uploaded material through Retrieval-Augmented Generation, the app
tells you when your notes don't cover something instead of inventing an answer.

## Features

* **Document upload & processing** — Upload course PDFs from your phone with a live progress
  bar. Text and diagrams are extracted, chunked, embedded locally, and stored in a per-user
  vector database.
* **Grounded Q&A (RAG)** — Ask questions about your notes and get streamed, Markdown-formatted
  answers built only from the passages retrieved from your own documents. If the notes don't
  contain the answer, the app says so.
* **Auto-generated quizzes** — Create multiple-choice practice tests of 3–20 questions from a
  document you choose, with answers and explanations drawn strictly from that material.
* **Flashcards** — Generate Quizlet-style tap-to-reveal flashcards from a chosen PDF for quick
  revision.
* **Study history** — Browse past quizzes and flashcard sets by source document and date; open
  any set read-only or delete it to reclaim space.
* **Progress tracking** — See uploads, quiz scores, overall accuracy, and how many flashcard
  answers you've revealed.
* **Per-user isolation** — Each account's documents live in a separate vector store, so one
  student's material never affects another's answers.

## Architecture & Tech Stack

StudyAI uses a decoupled, three-layer architecture: a Kotlin/Compose Android client, a Python
FastAPI service, and a data & inference layer combining a local embedding model, a per-user
vector store, and a cloud language-model provider. The client holds no API key and no database
path — it only sends an account id.

### Frontend (Android app)

* **Language:** Kotlin
* **UI framework:** Jetpack Compose (Material 3)
* **Networking:** OkHttp3 with Gson (multipart upload, and incremental reads of a
  server-sent-event stream for chat)
* **Navigation:** Two nested navigation graphs — an outer auth/onboarding host and an inner
  five-tab bottom-navigation host (Home, Chat, Quiz, Progress, Profile). A shared view model
  scoped to the signed-in graph keeps chat and quiz state alive across tab switches.

### Backend (AI & API)

* **Language:** Python
* **API framework:** FastAPI (asynchronous) served by Uvicorn, supervised by systemd
* **PDF extraction:** PyMuPDF
* **RAG pipeline:** LangChain `RecursiveCharacterTextSplitter` for chunking (500 characters,
  50-character overlap); cosine retrieval over the top 5 chunks for chat and top 12 for quizzes;
  random chunk sampling for flashcards
* **Embeddings:** Ollama serving `nomic-embed-text` **locally** (768-dimension vectors) — no
  document text ever leaves the workstation
* **Language model:** OpenRouter, calling `deepseek/deepseek-v4-flash` with automatic fallback to
  `google/gemini-2.5-flash-lite`. A single `LLM_PROVIDER` constant switches the whole system to
  local Ollama inference for fully offline operation.
* **Vector store:** ChromaDB, one collection per user, isolated by directory path
* **Relational storage:** SQLite (WAL mode) — four tables (`users`, `documents`, `quizzes`,
  `flashcards`) linked by user id with cascading deletes

### Deployment

The backend runs on a self-hosted Ubuntu workstation and is published at
`https://studyai.binodtiwari.com` through Nginx and a Cloudflare Tunnel opened outbound from the
machine — so no inbound port is opened on the network and the phone needs no VPN. The OpenRouter
API key is loaded from a gitignored `.env` file at startup and is never committed.

## Meet the Team

Developed as a senior design project at The University of Texas at Arlington (UTA).

* Binod Tiwari — Backend lead & AI integration
* Saujan Parajuli — Backend support, testing, and documentation
* Pushpa Raj Adhikari — Android frontend

## Getting Started

### Prerequisites

* [Android Studio](https://developer.android.com/studio) (latest version) with an emulator or a
  physical device on **Android 8.0 (API 26)** or higher
* **Python 3.10+**
* [Ollama](https://ollama.com) with the `nomic-embed-text` model pulled
* An OpenRouter API key

### 1. Running the FastAPI backend

```bash
# Clone the repository
git clone https://github.com/boyedandtoyed/StudyAI.git
cd StudyAI

# Create and activate a virtual environment
python3 -m venv venv
source venv/bin/activate

# Install Python dependencies
pip install -r requirements.txt

# Pull the local embedding model (Ollama must be installed and running)
ollama pull nomic-embed-text

# Provide your OpenRouter key (never commit this file)
cp .env.example .env
# then edit .env and set OPENROUTER_API_KEY=sk-or-v1-...

# Run the server
uvicorn main_fastapi:app --host 0.0.0.0 --port 8002
```

The database schema is created automatically on first startup; a fresh clone just works. In
production the server runs under a `systemd` unit rather than by hand.

### 2. Backend URL (Android app)

The app's backend address is set in one place:
`app/src/main/java/com/example/aistudyassistant/network/NetworkConfig.kt`

By default it points at the public backend, published through a Cloudflare Tunnel — no VPN
required on the phone:

```kotlin
const val BASE_URL = "https://studyai.binodtiwari.com"
```

**Tailscale fallback.** If the tunnel is down, comment out the line above and uncomment the
Tailscale fallback in the same file:

```kotlin
const val BASE_URL = "http://100.95.45.33:8002"
```

The fallback requires Tailscale running on the phone and connected to the same tailnet as the
backend host. Cleartext HTTP is otherwise blocked app-wide by
`app/src/main/res/xml/network_security_config.xml`, which carves out a cleartext exception for
that one Tailscale IP only. Once the tunnel is no longer a single point of failure, delete the
`domain-config` block from that file and remove the commented fallback line here.
