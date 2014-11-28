package uos.jhoffjann.server.controller;

import com.google.gson.Gson;
import org.apache.commons.fileupload.FileUploadException;
import org.bytedeco.javacpp.opencv_core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import uos.jhoffjann.server.common.AnalyzeResponse;
import uos.jhoffjann.server.common.ObjectStorage;
import uos.jhoffjann.server.common.Result;
import uos.jhoffjann.server.logic.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * SpringMVC Controller that lives on the server and handles incoming HTTP requests.
 */
@Controller
public class OCVController {

    private static final Logger log = LoggerFactory.getLogger(OCVController.class);
    private final String root = System.getProperty("user.dir");
    private final boolean DEBUG = true;
    private ExecutorService pool = Executors.newFixedThreadPool(10);
    private final String WikiURL = "http://de.wikipedia.org/wiki/";


    // array of supported extensions (use a List if you prefer)
    static final String[] EXTENSIONS = new String[]{
            "xml, json" // and other formats you need
    };
    // filter to identify images based on their extensions
    static final FilenameFilter IMAGE_FILTER = new FilenameFilter() {

        @Override
        public boolean accept(final File dir, final String name) {
            for (final String ext : EXTENSIONS) {
                if (name.endsWith("." + ext)) {
                    return (true);
                }
            }
            return (false);
        }
    };

    /**
     * @return
     */
    @RequestMapping(value = "/analyze", method = RequestMethod.GET)
    public
    @ResponseBody
    AnalyzeResponse analyzeRequest() {
        return new AnalyzeResponse("Hi! If you want a picture analyzed just post it to this url", new Date());
    }

    /**
     * @param name
     * @param image
     * @return
     */
    @RequestMapping(value = "/analyze", method = RequestMethod.POST)
    public
    @ResponseBody
    AnalyzeResponse analyzeRequest(@RequestParam("name") String name, @RequestParam("file") MultipartFile image) {
        log.info(new Date() + " - Request for Analyzing");
        try {
            if (!image.isEmpty()) {
                // Create a temporary directory to store the image
                // TODO check for images. Convert all images to .jpg

                File serverFile = Upload.uploadFile(root + File.separator + "tmpFiles", name, image);

                if (serverFile == null)
                    throw new FileUploadException("File upload was not successful!");

                log.info(new Date() + " - File was successfully uploaded!");

                opencv_core.Mat descriptors = OCV_Descriptor.getDescriptor(serverFile, DEBUG);

                Set<Future<Result>> set = new HashSet<Future<Result>>();

                File dir = new File(root + File.separator + "object");
                // TODO get xml from json and start a thread
                // start a thread for each image
                if (dir.isDirectory()) { // make sure it's a directory
                    for (final File f : dir.listFiles(IMAGE_FILTER)) {
                        log.info(new Date() + " - Starting Analyzing");
                        Gson gson = new Gson();
                        BufferedReader br = new BufferedReader(new FileReader(f));
                        ObjectStorage storage = gson.fromJson(br, ObjectStorage.class);
                        log.debug(new Date() + " - Now analyzing " + storage.getDescriptorPath());
                        Callable<Result> callable = new OCV_Matcher(storage.getName(),
                                Serializer.deserializeMat(storage.getDescriptorPath(), storage.getName()), descriptors);
                        Future<Result> future = pool.submit(callable);
                        set.add(future);
                    }
                }
                // check Results
                Result best = null;
                for (Future<Result> future : set) {
                    if (best == null)
                        best = future.get();
                    else if (best.getMatches().size() < future.get().getMatches().size()) {
                        best = future.get();
                    }
                }
                // return best Result, which has at least 4 matches
                if (best != null && best.getMatches().size() > 4) {
                    log.info(new Date() + " - Quantity of good matches: " + best.getMatches().size() + "");
                    // write best Result to json to make it better to understand
                    dir = new File(root + File.separator + "results");
                    if (!dir.exists())
                        dir.mkdirs();
                    // run save task in Thread to shorten response time
                    Runnable saver = new ResultSaver(dir, best);
                    pool.submit(saver);
                    // TODO get first n characters of Wikipedia-Article to give useable context-aware information
                    return new AnalyzeResponse("You're a looking at a " + best.getName(), new Date());
                } else {
                    return new AnalyzeResponse("Nothing found here", new Date());
                }

            } else {
                return new AnalyzeResponse("How about a picture?", new Date());
            }
        } catch (Exception e) {
            log.error(e.getMessage() + e.toString());
            e.printStackTrace();
            return new AnalyzeResponse("You probably did everything right. But there was an exception on the sever", new Date());
        }

    }

    /**
     * @return
     */
    @RequestMapping(value = "/add", method = RequestMethod.GET)
    public
    @ResponseBody
    AnalyzeResponse addRequest() {
        return new AnalyzeResponse("Hi! If you want a picture analyzed just post it to this url", new Date());
    }

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public
    @ResponseBody
    AnalyzeResponse addObject(@RequestParam("name") String name, @RequestParam("file") MultipartFile image) {
        log.info("Request for object adding");
        try {
            if (!image.isEmpty()) {
                // TODO check for images. Convert all images to .jpg
                File serverFile = Upload.uploadFile(root + File.separator + "object_images", name, image);

                if (serverFile == null)
                    throw new FileUploadException("There was a problem with the FileUpload");

                opencv_core.Mat descriptors = OCV_Descriptor.getDescriptor(serverFile, DEBUG);
                String xml = Serializer.serializeMat(name, descriptors);

                // create JSON to store the whole thing
                ObjectStorage objectStorage = new ObjectStorage(name, xml, new Date(),
                        WikiHandler.getPlainSummary(WikiURL + name));

                Gson gson = new Gson();
                String json = gson.toJson(objectStorage);

                File dir = new File(root + File.separator + "object");
                if (!dir.exists())
                    dir.mkdirs();

                //write object to HDD
                FileWriter writer = new FileWriter(dir.getAbsolutePath() + File.separator
                        + new SimpleDateFormat("yyyyMMddhhmmss").format(new Date()) + "-" +
                        name.toLowerCase().replaceAll("\\s+","") + ".json");
                writer.write(json);
                writer.close();

                log.info(new Date() + " - File was successfully uploaded!");

                return new AnalyzeResponse("Wohoo. I got a picture.", new Date());

            } else {
                return new AnalyzeResponse("How about a picture?", new Date());
            }
        } catch (Exception e) {
            log.error(e.getMessage() + e.toString());
            e.printStackTrace();
            return new AnalyzeResponse("You probably did everything right. But there was an exception on the sever", new Date());
        }

    }

}
