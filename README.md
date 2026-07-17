# 📚 AI-Powered Study Assistant for Students

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![Python](https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-005571?style=for-the-badge&logo=fastapi)
![PyTorch](https://img.shields.io/badge/PyTorch-EE4C2C?style=for-the-badge&logo=pytorch&logoColor=white)

An intelligent mobile application that helps students learn more effectively by allowing them to upload their study materials (notes, PDFs) and interacting with an AI to answer questions, generate practice quizzes, and identify review topics. 

---

## 🚀 Features

*   **📄 Document Upload & Processing:** Securely upload course PDFs and text notes directly from your mobile device.
*   **🤖 Context-Aware Q&A (RAG):** Ask complex questions about your notes and get accurate, hallucination-free answers using Retrieval-Augmented Generation.
*   **📝 Auto-Generated Quizzes:** Instantly create practice tests based strictly on your uploaded content to test your knowledge before exams.
*   **📊 Knowledge Gap Analysis:** The AI tracks your quiz performance to identify specific topics you need to review more thoroughly.

---

## 🛠️ Architecture & Tech Stack

This project uses a decoupled architecture, separating the modern mobile frontend from the heavy machine-learning backend.

### Frontend (Android App)
*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose (Declarative UI)
*   **Network:** Retrofit / Ktor (for API communication)

### Backend (AI & API)
*   **Language:** Python
*   **API Framework:** FastAPI (Asynchronous, high-performance)
*   **AI/ML Integration:** PyTorch & Local LLMs
*   **RAG Pipeline:** LangChain / LlamaIndex (for chunking, embedding, and vector storage)

### Design Patterns Utilized
*   **Strategy Pattern:** Used to swap between different LLM models or embedding strategies.
*   **Chain of Responsibility:** Used in the backend to process user queries through multiple validation and retrieval steps before generating a final answer.

---

## 👥 Meet the Team

Developed for our semester team project at **The University of Texas at Arlington (UTA)**.

*   **Pushpa Raj Adhikari**
*   **[Binod Tiwari]** 
*   **[Saujan Parajuli]** 

---

## ⚙️ Getting Started

### Prerequisites
*   [Android Studio](https://developer.android.com/studio) (Latest Version)
*   Python 3.9+
*   An Android Emulator or physical device (Minimum SDK 24)

### 1. Running the FastAPI Backend
```bash
# Clone the repository
git clone https://github.com/your-username/AI-Study-Assistant.git

# Navigate to the backend directory
cd AI-Study-Assistant/backend

# Install Python dependencies
pip install -r requirements.txt

# Run the FastAPI server
uvicorn main:app --reload
```

### 2. Backend URL

The Android app's backend address is set in one place:
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
