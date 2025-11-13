import java.util.List;

import com.openai.client.OpenAIClient;
import com.openai.models.beta.assistants.Assistant;
import com.openai.models.beta.assistants.AssistantCreateParams;
import com.openai.models.beta.threads.Thread;
import com.openai.models.beta.threads.ThreadCreateParams;
import com.openai.models.beta.threads.ThreadCreateParams.ToolResources;
import com.openai.models.beta.threads.ThreadCreateParams.ToolResources.FileSearch;
import com.openai.models.beta.threads.runs.Run;
import com.openai.models.beta.threads.runs.RunCreateParams;

public class AssistantCompare {
  private Assistant assistant;

  public AssistantCompare(OpenAIClient client) {
    assistant = client.beta().assistants().create(
      AssistantCreateParams.builder()
        .name("Assistant")
        .model("gpt-4")
        .build()
    );
  }

  public String compare(OpenAIClient client, String fileId1, String fileId2) {
      Thread thread = client.beta().threads().create(
        ThreadCreateParams.builder()
            .toolResources(
                ToolResources.builder()
                    .fileSearch(
                        FileSearch.builder()
                            .vectorStoreIds(List.of(fileId1, fileId2))
                            .build()
                    )
                    .build()
            )
            .build()
      );

      Run run = client.beta().threads().runs().create(
        thread.id(),
        RunCreateParams.builder()
            .assistantId(assistant.id())
            .build()
      );

      return run.toString();
  }
}