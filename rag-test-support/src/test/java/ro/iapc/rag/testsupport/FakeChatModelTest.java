package ro.iapc.rag.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

class FakeChatModelTest {

    @Test
    void reportsWordCountsAsTokenUsageByDefault() {
        ChatResponse response = new FakeChatModel("three word answer").call(new Prompt("five words in this prompt"));

        assertThat(response.getMetadata().getUsage().getPromptTokens()).isEqualTo(5);
        assertThat(response.getMetadata().getUsage().getCompletionTokens()).isEqualTo(3);
    }

    @Test
    void reportsFixedUsageWhenTold() {
        FakeChatModel model = new FakeChatModel();
        model.reportUsage(1200, 80);

        ChatResponse response = model.call(new Prompt("q"));

        assertThat(response.getMetadata().getUsage().getPromptTokens()).isEqualTo(1200);
        assertThat(response.getMetadata().getUsage().getCompletionTokens()).isEqualTo(80);
    }

    @Test
    void leavesUsageOutWhenTold() {
        FakeChatModel model = new FakeChatModel();
        model.reportNoUsage();

        ChatResponse response = model.call(new Prompt("q"));

        // Spring AI fills in an empty usage with zeros when a model reports none.
        assertThat(response.getMetadata().getUsage().getTotalTokens()).isZero();
        assertThat(response.getResult().getOutput().getText()).isEqualTo("fake answer");
    }
}
