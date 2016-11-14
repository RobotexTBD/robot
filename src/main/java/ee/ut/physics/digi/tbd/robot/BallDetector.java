package ee.ut.physics.digi.tbd.robot;

import ee.ut.physics.digi.tbd.robot.image.ColoredImage;
import ee.ut.physics.digi.tbd.robot.image.GrayscaleImage;
import ee.ut.physics.digi.tbd.robot.image.processing.ImageProcessorService;
import ee.ut.physics.digi.tbd.robot.matrix.VisitedMap;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;

@Slf4j
public class BallDetector {

    private final ImageProcessorService imageProcessorService;

    public BallDetector(ImageProcessorService imageProcessorService) {
        this.imageProcessorService = imageProcessorService;
    }

    public Collection<Blob> findBalls(ColoredImage hsvImage) {
        GrayscaleImage certaintyMap = imageProcessorService.generateBallCertaintyMap(hsvImage);
        return findBalls(certaintyMap);
    }

    private Collection<Blob> findBalls(GrayscaleImage certaintyMap) {
        long time = System.currentTimeMillis();
        VisitedMap visitedMap = new VisitedMap(certaintyMap.getWidth(), certaintyMap.getHeight());
        Collection<Blob> maxCertaintyBlobs = getMaxCertaintyBlobs(certaintyMap, visitedMap);
        log.debug("Finding balls took " + (System.currentTimeMillis() - time) + " milliseconds");
        return maxCertaintyBlobs;
    }

    private Collection<Blob> getMaxCertaintyBlobs(GrayscaleImage certaintyMap, VisitedMap visitedMap) {
        Collection<Blob> balls = new ArrayList<>();
        int visitedMapPos = visitedMap.getStartIndex();
        int maxCertaintyPos = certaintyMap.getStartIndex();
        for(int y = 0; y < visitedMap.getHeight(); y++) {
            for(int x = 0; x < visitedMap.getWidth(); x++, visitedMapPos++, maxCertaintyPos++) {
                if(!(certaintyMap.getData()[maxCertaintyPos] >= 205) || visitedMap.getVisited()[visitedMapPos]) {
                    continue;
                }
                Blob ball = findBall(visitedMapPos, visitedMap, certaintyMap);
                if(ball != null) {
                    balls.add(ball);
                }
            }
            visitedMapPos += visitedMap.getStride() - visitedMap.getWidth();
            maxCertaintyPos += certaintyMap.getStride() - visitedMap.getWidth();
        }
        return balls;
    }

    private Blob findBall(int visitedMapStartPos, VisitedMap visitedMap, GrayscaleImage certaintyMap) {
        Queue<Integer> visitQueue = new ArrayDeque<>();
        visitQueue.add(visitedMapStartPos);
        int count = 0;
        int sumX = 0;
        int sumY = 0;
        while(!visitQueue.isEmpty()) {
            int visitedMapPos = visitQueue.poll();
            if(visitedMap.getVisited()[visitedMapPos]) {
                continue;
            }
            int x = visitedMap.getX(visitedMapPos);
            int y = visitedMap.getY(visitedMapPos);
            if(certaintyMap.getData()[x + y * certaintyMap.getStride()] < 205) {
                continue;
            }
            visitedMap.getVisited()[visitedMapPos] = true;
            count++;
            sumX += x;
            sumY += y;
            int[] visitedMapMoves = new int[] {-1, 1, -visitedMap.getStride(), visitedMap.getStride()};
            int[] certaintyMapMoves = new int[] {-1, 1, -certaintyMap.getStride(), certaintyMap.getStride()};
            for(int i = 0; i < 4; i++) {
                int newVisitedMapPos = visitedMapPos + visitedMapMoves[i];
                int newMaxCertaintyPos = x + y * certaintyMap.getStride() + certaintyMapMoves[i];
                if(!visitedMap.getVisited()[newVisitedMapPos] && certaintyMap.getData()[newMaxCertaintyPos] >= 205) {
                    visitQueue.add(newVisitedMapPos);
                }
            }
        }
        if(count > 64) {
            return new Blob(sumX / count, sumY / count, count);
        }
        return null;
    }


}
