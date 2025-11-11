import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import javax.servlet.http.Part;

import com.openai.client.OpenAIClient;
import com.openai.models.files.FileCreateParams;
import com.openai.models.files.FileObject;
import com.openai.models.files.FilePurpose;

public class HandleFiles {
  
  public static FileObject upload(OpenAIClient client, Part file) {
    try {
        // Download file from URL
        // byte[] fileBytes = url.openStream().readAllBytes();

        // Create temp file directly
        Path tempFile = Files.createTempFile("openai-upload-", ".pdf");
        
        // Write uploaded content directly to temp file
        try (var input = file.getInputStream()) {
            long bytesCopied = Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Bytes copied: " + bytesCopied);
        }

        // Create temp file (will be deleted immediately after upload)
        // Path tempFile = Files.createTempFile("upload", ".tmp");
        // Files.write(tempFile, fileBytes);

        try {
            // Upload to OpenAI
            FileObject fileObject = client.files().create(
                FileCreateParams.builder()
                    .file(tempFile)
                    .purpose(FilePurpose.ASSISTANTS)
                    .build()
            );

            return fileObject;
        } finally {
            Files.deleteIfExists(tempFile);
        }
        
    } catch (Exception e) {
        throw new RuntimeException(e);
    } 
  }

  public static List<FileObject> view(OpenAIClient client) {
     return client.files().list().data();
  }

  public static void delete(OpenAIClient client, String fileId) {
    client.files().delete(fileId);
  }
}