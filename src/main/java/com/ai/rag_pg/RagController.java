package com.ai.rag_pg;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
public class RagController {

    private final VectorStore vectorStore;
    private final OllamaChatModel chatModel;

    public RagController(VectorStore vectorStore, OllamaChatModel chatModel) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
    }

    private final TokenTextSplitter textSplitter = TokenTextSplitter
            .builder().withChunkSize(400)
            .withMaxNumChunks(3000)
            .withKeepSeparator(true)
            .withMinChunkLengthToEmbed(20)
            .withMinChunkSizeChars(200).build();

    @PostMapping("/input/document")
    public CompletableFuture<String> addDocuments(@RequestParam("files") List<MultipartFile> files) {
//indexChecker.waitForIndexReady("admin", "vector_store", "vector_index", 300);
        return CompletableFuture.runAsync(() -> {
            List<Document> documents = new ArrayList<>();

            files.forEach(file -> {
                try {
                    String content;

                    if (file.getOriginalFilename() != null &&
                            file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {

                        // Read PDF content
                        File tempFile = File.createTempFile("upload-", ".pdf");
                        file.transferTo(tempFile);
                        try (PDDocument pdfDoc = Loader.loadPDF(tempFile)) {
                            PDFTextStripper stripper = new PDFTextStripper();
                            content = stripper.getText(pdfDoc);
                        }

                    } else {
                        // Read text or other file content
                        content = new String(file.getBytes());
                    }

                    // Create initial Document
                    Document doc = new Document(content);

                    // Split into smaller chunks
                    List<Document> chunks = textSplitter.split(doc);

                    // Add chunks to main list
                    documents.addAll(chunks);

                } catch (Exception e) {
                    throw new RuntimeException("Failed to read file: " + file.getOriginalFilename(), e);
                }
            });
            vectorStore.accept(documents);

        }).thenApply(value -> "Documents added successfully");
    }

    @GetMapping("/answer")
    public String getAnswer(@RequestParam String query) {

        // 1️⃣ Retrieve top chunks manually
        List<Document> relevantDocs = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(3)
                .build());

        // 2️⃣ Build the context string
        String context = relevantDocs.stream()
                .map(Document::getFormattedContent)
                .reduce("", (a, b) -> a + "\n---\n" + b);

        // 3️⃣ Pass context into the system prompt
        return ChatClient.builder(chatModel).build().prompt()
                .system(STR."""
You are an AI assistant.
Use ONLY the information below (CONTEXT) to answer the QUESTION.
If you cannot find the answer, say "I don't know."
Be concise and factual.

CONTEXT:
\{context}""")
                .user(query)
                .call()
                .content();
    }

}
