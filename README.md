StudyMind AI — an AI-powered study assistant that turns notes and PDFs into interactive learning material.

What it does:

· 📝 Create and organize study notes
· 📄 Upload PDFs → auto-parsed, chunked, and embedded
· 💬 Chat with your material (RAG-grounded answers with citations, streamed live)
· ❓ Auto-generate multiple-choice quizzes from your content
· 🗂️ Auto-generate flashcards with SM-2 spaced repetition
· 📊 Track progress (streaks, scores, activity timeline)

Stack (all free tier):

Layer Tech
Frontend React 18 + Vite + TypeScript, deployed on Vercel
Backend FastAPI (Python 3.11) in Docker, deployed on Render
Database + Auth + Storage Supabase (Postgres + pgvector, JWT auth, file storage)
AI Google Gemini (gemini-1.5-flash + text-embedding-004)

Live URLs:

· App: https://frontend-pdri.vercel.app
· API: https://studymind-api-7d17.onrender.com
· Docs: https://studymind-api-7d17.onrender.com/docs

Architecture:

· 21 REST endpoints across 6 routers (notes, documents, chat, quizzes, flashcards, progress)
· Vector search via pgvector (768-dim embeddings, cosine similarity, HNSW index)
· SSE streaming for chat (custom ReadableStream consumer on the client)
· 12 Postgres tables with RLS, 6 SQL migrations
· Structured JSON logging, rate limiting on Gemini calls, retry with exponential backoff
· UptimeRobot keeps the Render instance warm

Scale: ~37 backend files, ~35 frontend files. Built entirely from an Android phone.

Current status: Backend fully deployed and verified. Frontend builds and renders, but signup/signin fails with "Failed to fetch" — Supabase env vars in Vercel need verification.
