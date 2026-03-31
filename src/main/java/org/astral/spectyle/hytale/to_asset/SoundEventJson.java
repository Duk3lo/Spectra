package org.astral.spectyle.hytale.to_asset;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

public record SoundEventJson(
        @SerializedName("Parent") String parent,
        @SerializedName("Volume") int volume,
        @SerializedName("Layers") List<Layer> layers,
        @SerializedName("AbsolutePath") String absolutePath
) {
    public record Layer(
            @SerializedName("Files") List<String> files,
            @SerializedName("Volume") int volume,
            @SerializedName("RandomSettings") RandomSettings randomSettings
    ) {}

    public record RandomSettings(
            @SerializedName("MinPitch") int minPitch,
            @SerializedName("MaxPitch") int maxPitch
    ) {}

    public static @NotNull SoundEventJson create(String oggPathInCommonSounds, @NotNull Path absolutePath) {
        return new SoundEventJson(
                "SFX_Attn_Moderate",
                0,
                List.of(new Layer(
                        List.of(oggPathInCommonSounds),
                        0,
                        new RandomSettings(0, 0)
                )),
                absolutePath.toString()
        );
    }
}