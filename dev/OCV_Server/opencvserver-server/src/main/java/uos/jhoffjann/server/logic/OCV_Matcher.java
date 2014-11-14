package uos.jhoffjann.server.logic;


import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_features2d;
import uos.jhoffjann.server.common.Result;

import java.util.ArrayList;
import java.util.concurrent.Callable;


/**
 * Created by jhoffjann on 04.11.14.
 */
public class OCV_Matcher implements Callable<Result> {


    // Create Matcher
    private opencv_features2d.FlannBasedMatcher matcher = new opencv_features2d.FlannBasedMatcher();

    private final double RATIO = 0.65;

    private String name;

    opencv_core.Mat descriptors[] = {new opencv_core.Mat(), new opencv_core.Mat()};

    public OCV_Matcher(String name, opencv_core.Mat desc1, opencv_core.Mat desc2) {
        descriptors[0] = desc1;
        descriptors[1] = desc2;
        this.name = name;
    }

    /**
     * @param matches
     * @return
     */
    private ArrayList<opencv_features2d.DMatch> getGoodMatches(opencv_features2d.DMatchVectorVector matches) {
        ArrayList<opencv_features2d.DMatch> goodMatches = new ArrayList<opencv_features2d.DMatch>();
        for (int j = 0; j < matches.size(); j++) {
            double mRatio = matches.get(j, 0).distance() / matches.get(j, 1).distance();
            // System.out.println(matches.get(j, 0).distance() + " / " + matches.get(j, 1).distance() + " = " + mRatio);

            if (mRatio <= RATIO) {
                goodMatches.add(matches.get(j, 0));
            }
        }
        return goodMatches;

    }

    /**
     *
     */
    @Override
    public Result call() {
        // Match it
        opencv_features2d.DMatchVectorVector matches = new opencv_features2d.DMatchVectorVector();
        matcher.knnMatch(descriptors[0], descriptors[1], matches, 2);
        // filter for "good matches"
        opencv_features2d.DMatchVectorVector dMatches = new opencv_features2d.DMatchVectorVector();

        ArrayList<opencv_features2d.DMatch> goodMatches = getGoodMatches(matches);

        return new Result(name, goodMatches.size());
    }
}