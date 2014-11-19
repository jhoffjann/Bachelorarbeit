package uos.jhoffjann.server.logic;

import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

/**
 * Created by Jannik on 19.11.14.
 */
public class Upload {

    // Uploads a MultipartFile to a specified path

    /**
     *
     * @param path
     * @param name
     * @param file
     * @return
     */

    public static File uploadFile(String path, String name, MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            File tmpDir = new File(path);
            if (!tmpDir.exists())
                tmpDir.mkdirs();
            // Create file on server
            File serverFile = new File(tmpDir.getAbsolutePath() + File.separator + name + new Date() + ".jpg");
            BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(serverFile));
            stream.write(bytes);
            stream.close();
            return serverFile;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
