const API_BASE = "/api/v1";

const docsEl = document.getElementById("docs");
const docCountEl = document.getElementById("doc-count");
const uploadForm = document.getElementById("upload-form");
const uploadStatus = document.getElementById("upload-status");
const refreshBtn = document.getElementById("refresh-btn");
const clearAllBtn = document.getElementById("clear-all-btn");
const askBtn = document.getElementById("ask-btn");
const askStatus = document.getElementById("ask-status");
const answerEl = document.getElementById("answer");
const citationsEl = document.getElementById("citations");

// confirmation modal elements
const confirmModal = document.getElementById("confirm-modal");
const confirmOk = document.getElementById("confirm-ok");
const confirmCancel = document.getElementById("confirm-cancel");

// System status panel logic
const statsEl = document.getElementById("stats");
const statsRefreshBtn = document.getElementById("stats-refresh");
const geminiBtn = document.getElementById("gemini-check");
const embedBtn = document.getElementById("embed-check");
const geminiStatusEl = document.getElementById("gemini-status");

async function fetchJSON(url, options){
  const r = await fetch(url, options);
  if(!r.ok) throw new Error(`HTTP ${r.status}`);
  return r.json();
}

function renderDocs(list){
  docsEl.innerHTML = "";
  list.forEach(d => {
    const div = document.createElement("div");
    div.className = "doc";
    div.innerHTML = `
      <input type="checkbox" data-id="${d.id}" />
      <div class="meta">
        <strong>${d.file_name}</strong><br/>
        <small>Pages: ${d.page_count} • Size: ${(d.size_bytes/1024).toFixed(1)} KB • ID: ${d.id}</small>
      </div>
      <div class="actions">
        <a href="${API_BASE}/documents/${d.id}/file" target="_blank" title="View">PDF</a>
      </div>`;
    docsEl.appendChild(div);
  });
  docCountEl.textContent = `${list.length} document(s)`;
}

async function loadDocs(){
  try {
    refreshBtn.disabled = true;
    const list = await fetchJSON(`${API_BASE}/documents`);
    renderDocs(list);
  } catch(e){
    docsEl.innerHTML = `<div class="status">Failed to load: ${e.message}</div>`;
  } finally {
    refreshBtn.disabled = false;
  }
}

uploadForm.addEventListener("submit", async e => {
  e.preventDefault();
  const file = document.getElementById("file-input").files[0];
  if(!file){ return; }
  uploadStatus.textContent = "Uploading...";
  const fd = new FormData();
  fd.append("file", file);
  try {
    const res = await fetchJSON(`${API_BASE}/documents`, { method: "POST", body: fd });
    uploadStatus.textContent = `Uploaded: ${res.file_name} (ID ${res.id})`;
    document.getElementById("file-input").value = "";
    loadDocs();
  } catch(e){
    uploadStatus.textContent = `Upload failed: ${e.message}`;
  }
});

refreshBtn.addEventListener("click", loadDocs);

// Helper to perform destructive clear action
async function performClearAll(){
  clearAllBtn.disabled = true;
  try {
    const resp = await fetch(`${API_BASE}/documents`, { method: 'DELETE' });
    if(!resp.ok) throw new Error(`HTTP ${resp.status}`);
    await loadDocs();
    await loadStats();
    alert('All documents and chunks have been removed successfully.');
  } catch(e){
    alert('Failed to clear data: ' + e.message);
  } finally {
    clearAllBtn.disabled = false;
  }
}

// Clear All button: show confirmation modal or fallback to native confirm
clearAllBtn.addEventListener("click", () => {
  if (confirmModal && confirmOk && confirmCancel) {
    confirmModal.style.display = "block";
    confirmModal.setAttribute("aria-hidden", "false");
  } else {
    const ok = window.confirm("This will permanently delete all documents, chunks, and uploaded files. This action cannot be undone. Proceed?");
    if (ok) {
      performClearAll();
    }
  }
});

confirmCancel?.addEventListener("click", () => {
  if(!confirmModal) return;
  confirmModal.style.display = "none";
  confirmModal.setAttribute("aria-hidden", "true");
});

confirmOk?.addEventListener("click", async () => {
  if(!confirmModal) return;
  confirmOk.disabled = true;
  confirmCancel.disabled = true;
  try {
    await performClearAll();
  } finally {
    confirmOk.disabled = false;
    confirmCancel.disabled = false;
    confirmModal.style.display = "none";
    confirmModal.setAttribute("aria-hidden", "true");
  }
});

askBtn.addEventListener("click", async () => {
  const question = document.getElementById("question").value.trim();
  const topK = parseInt(document.getElementById("topk").value, 10) || 5;
  const translate = document.getElementById("translate").checked;
  if(!question){ askStatus.textContent = "Please enter a question."; return; }
  const selected = Array.from(docsEl.querySelectorAll("input[type=checkbox]:checked")).map(cb => parseInt(cb.dataset.id,10));
  askStatus.textContent = "Asking...";
  answerEl.textContent = "";
  citationsEl.innerHTML = "";
  askBtn.disabled = true;
  try {
    const body = { question, top_k: topK };
    if(selected.length) body.document_ids = selected;
    if(translate) body.translate_sources = true;
    const res = await fetchJSON(`${API_BASE}/chat/ask`, { method: "POST", headers: {"Content-Type":"application/json"}, body: JSON.stringify(body)});
    answerEl.textContent = res.answer;
    if(Array.isArray(res.citations)){
      res.citations.forEach(c => {
        const div = document.createElement("div");
        div.className = "cite";
        div.innerHTML = `<small>[${c.file_name} p.${c.page_number}]</small><br/>${escapeHtml(c.snippet)}${c.translated_snippet ? `<hr/><em>${escapeHtml(c.translated_snippet)}</em>`:""}`;
        citationsEl.appendChild(div);
      });
    }
    askStatus.textContent = "Done.";
  } catch(e){
    askStatus.textContent = `Error: ${e.message}`;
  } finally {
    askBtn.disabled = false;
  }
});

function escapeHtml(str){
  return str.replace(/[&<>]/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;"}[c]));
}

async function loadStats(){
  try {
    statsRefreshBtn.disabled = true;
    const data = await fetchJSON(`${API_BASE}/debug/stats`);
    statsEl.textContent = `Documents: ${data.documents} • Chunks: ${data.chunks}`;
  } catch(e){
    statsEl.textContent = `Stats error: ${e.message}`;
  } finally {
    statsRefreshBtn.disabled = false;
  }
}
statsRefreshBtn?.addEventListener("click", loadStats);

geminiBtn?.addEventListener("click", async ()=>{
  geminiStatusEl.textContent = "Checking Gemini...";
  try {
    const r = await fetch(`${API_BASE}/test/gemini`);
    const text = await r.text();
    if(!r.ok) throw new Error(r.status);
    geminiStatusEl.textContent = text;
  } catch(e){
    geminiStatusEl.textContent = `Gemini test failed: ${e.message}`;
  }
});

embedBtn?.addEventListener("click", async ()=>{
  geminiStatusEl.textContent = "Embedding test...";
  try {
    const r = await fetch(`${API_BASE}/test/embed`);
    const text = await r.text();
    if(!r.ok) throw new Error(r.status);
    geminiStatusEl.textContent = text;
  } catch(e){
    geminiStatusEl.textContent = `Embedding test failed: ${e.message}`;
  }
});

loadStats();

loadDocs();
