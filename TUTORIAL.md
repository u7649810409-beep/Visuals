# Ruffy Client (Minecraft 1.21.11) -> JAR ueber GitHub bauen

1. github.com -> New repository -> Name "ruffy-client" -> Create (leer lassen, keine README anlegen)
2. Zip entpacken. Im Repo-Seite "uploading an existing file" klicken und ALLE Dateien/Ordner
   aus dem entpackten Ordner hineinziehen (auch den versteckten Ordner .github!) -> Commit changes
3. Tab "Actions" -> "Build" laeuft automatisch (ca. 3-5 Min). Gruener Haken = fertig.
4. Auf den Run klicken -> unten bei "Artifacts" -> "ruffy-client-jar" herunterladen -> entpacken
5. ruffy-client-1.0.0.jar in .minecraft/mods legen (Fabric Loader 0.18.4+ fuer 1.21.11 installiert)
6. Fuer ein offizielles Release: Releases -> Create new release -> Tag "v1.0.0" -> Publish.
   Die Action haengt die JAR dann automatisch an.
