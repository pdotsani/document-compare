import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.openai.client.OpenAIClient;
import com.openai.models.files.FileCreateParams;
import com.openai.models.files.FileObject;
import com.openai.models.files.FilePurpose;

public class HandleFiles {
  
  public FileObject upload(OpenAIClient client, URL url) {
    try {
        // Download file from URL
        byte[] fileBytes = url.openStream().readAllBytes();

        // Create temp file (will be deleted immediately after upload)
        Path tempFile = Files.createTempFile("upload", ".tmp");
        Files.write(tempFile, fileBytes);

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

  public List<FileObject> view(OpenAIClient client) {
     return client.files().list().data();
  }

  public void delete(OpenAIClient client, String fileId) {
    client.files().delete(fileId);
  }
}