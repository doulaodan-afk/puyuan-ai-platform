package com.puyuanmaoshan.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

public final class PluginDtos {
    private PluginDtos() {}

    // AI Image Generation Request
    public record AiImageGenRequest(
            @NotBlank String prompt,
            @Pattern(regexp = "256x256|512x512|1024x1024|1792x1024|1024x1792", message = "Invalid image size")
            @JsonProperty("image_size") String imageSize,
            @JsonProperty(defaultValue = "standard") String quality
    ) {}

    // AI Image Generation Response
    public record AiImageGenResponse(
            String imageUrl,
            @JsonProperty("image_size") String imageSize,
            long tokenUsed,
            @JsonProperty("balance_remaining") long balanceRemaining
    ) {}

    // AI Script Generation Request
    public record AiScriptGenRequest(
            @NotBlank @JsonProperty("product_desc") String productDesc,
            @JsonProperty("product_url") String productUrl,
            @JsonProperty(defaultValue = "video") String scriptType
    ) {}

    // AI Script Generation Response
    public record AiScriptGenResponse(
            String script,
            @JsonProperty("script_type") String scriptType,
            long tokenUsed,
            @JsonProperty("balance_remaining") long balanceRemaining
    ) {}

    // AI Translate Request
    public record AiTranslateRequest(
            @NotBlank String text,
            @NotBlank @JsonProperty("target_lang") String targetLang
    ) {}

    // AI Translate Response
    public record AiTranslateResponse(
            String translatedText,
            @JsonProperty("source_lang") String sourceLang,
            @JsonProperty("target_lang") String targetLang,
            long tokenUsed,
            @JsonProperty("balance_remaining") long balanceRemaining
    ) {}

    // Unified Plugin Invoke Request
    public record PluginInvokeRequest(
            Map<String, Object> payload
    ) {}

    // Unified Plugin Invoke Response
    public record PluginInvokeResponse(
            Map<String, Object> data,
            @JsonProperty("token_used") long tokenUsed,
            @JsonProperty("balance_remaining") long balanceRemaining
    ) {}
}