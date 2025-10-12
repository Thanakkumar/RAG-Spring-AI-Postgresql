package com.ai.rag_pg;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RestController
public class RagController {

    private final VectorStore vectorStore;
    private final OllamaChatModel chatModel;

    public RagController(VectorStore vectorStore, OllamaChatModel chatModel) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
    }

    private final TokenTextSplitter textSplitter = new TokenTextSplitter();

    @PostMapping("/input/document")
    public String addDocuments(@RequestParam("files") List<MultipartFile> files) {
//indexChecker.waitForIndexReady("admin", "vector_store", "vector_index", 300);
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
        return "Documents added successfully";
    }

    @GetMapping("/answer")
    public String getAnswer(@RequestParam String query) {
        return ChatClient.builder(chatModel).build().prompt()
                .advisors(new QuestionAnswerAdvisor(vectorStore)).user(query).call().content();
    }

}
