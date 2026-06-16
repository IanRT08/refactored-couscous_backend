package mx.edu.cenidet.estadias.dtos.client;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThingSpeakFeedDTO {

    @JsonProperty("channel")
    private Channel channel;

    @JsonProperty("feeds")
    private List<Feed> feeds;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Channel {

        @JsonProperty("id")
        private Integer id;

        @JsonProperty("field1")
        private String field1;

        @JsonProperty("field2")
        private String field2;

        @JsonProperty("field3")
        private String field3;

        @JsonProperty("field4")
        private String field4;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Feed {

        @JsonProperty("created_at")
        private String createdAt;

        @JsonProperty("field1")
        private String field1;

        @JsonProperty("field2")
        private String field2;

        @JsonProperty("field3")
        private String field3;

        @JsonProperty("field4")
        private String field4;

    }

}
