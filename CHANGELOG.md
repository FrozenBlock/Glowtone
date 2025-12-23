Please clear changelog after each release.
Put the changelog BELOW the dashes. ANYTHING ABOVE IS IGNORED.
-----------------
- Fixed Observers using the incorrect render type, now being able to display their emissive layers properly.
- Glowtone overlay textures that are completely transparent will no longer be loaded.
  - This means the texture will not be added to and indexed in Minecraft's texture atlas.
  - This also means that blocks which would've received an additional overlay model face for the texture will no longer add these additional faces.
