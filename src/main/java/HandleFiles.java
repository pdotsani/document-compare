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
        // Create temp file directly
        String fileName = file.getSubmittedFileName();
        Path tempFile = Files.createTempFile(String.format("%s-", fileName), ".pdf");
        
        // Write uploaded content directly to temp file
        try (var input = file.getInputStream()) {
            Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

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