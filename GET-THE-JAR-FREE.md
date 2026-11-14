# Get the finished .jar for free (no installs)

GitHub builds the mod for you on its own computers. It costs nothing.

1. Make a free account at https://github.com and click **New repository**. Name it `tornadomod`, set it to **Public**, and create it.
2. On the new repository page click **uploading an existing file**.
3. Unzip this project on your computer, open the folder, select **everything inside it** (including the hidden `.github` folder) and drag it into the upload box.
   - Tip: if you can't see `.github`, turn on "show hidden files" in your file manager.
4. Click **Commit changes**.
5. Open the **Actions** tab. A run called "Build mod jar" starts by itself (about 5 to 10 minutes the first time).
6. When it shows a green tick, click the run, scroll to **Artifacts** and download the zip. The `.jar` mod file is inside.
7. Put that `.jar` in your Minecraft `mods` folder.

If the run turns red, open it, copy the error text and send it to Claude. It's usually a one-line fix.
