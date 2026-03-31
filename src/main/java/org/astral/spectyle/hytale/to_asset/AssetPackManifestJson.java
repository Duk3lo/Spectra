package org.astral.spectyle.hytale.to_asset;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record AssetPackManifestJson(
        @SerializedName("Group") String group,
        @SerializedName("Name") String name,
        @SerializedName("Version") String version,
        @SerializedName("Description") String description,
        @SerializedName("Authors") List<Author> authors,
        @SerializedName("IncludesAssetPack") boolean includesAssetPack
) {
    public record Author(
            @SerializedName("Name") String name
    ) {}

    public static @NotNull AssetPackManifestJson create(AssetPackBuilder.@NotNull AssetPackConfig config, String version) {
        return new AssetPackManifestJson(
                config.groupName(),
                config.packName(),
                version,
                config.description(),
                List.of(new Author(config.authorName())),
                true
        );
    }

    public boolean matches(AssetPackManifestJson other) {
        if (other == null) return false;
        return group.equals(other.group)
                && name.equals(other.name)
                && version.equals(other.version)
                && description.equals(other.description)
                && includesAssetPack == other.includesAssetPack
                && authors.equals(other.authors);
    }
}