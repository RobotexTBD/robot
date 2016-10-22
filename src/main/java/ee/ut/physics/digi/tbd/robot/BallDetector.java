package ee.ut.physics.digi.tbd.robot;

import ee.ut.physics.digi.tbd.robot.kernel.BallDetectorKernel;
import ee.ut.physics.digi.tbd.robot.kernel.ThresholderKernel;
import ee.ut.physics.digi.tbd.robot.matrix.image.BinaryImage;
import ee.ut.physics.digi.tbd.robot.matrix.image.ColoredImage;
import ee.ut.physics.digi.tbd.robot.matrix.image.GrayscaleImage;
import ee.ut.physics.digi.tbd.robot.matrix.VisitedMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;

public class BallDetector {

    private final BallDetectorKernel ballDetectorKernel;
    private final ThresholderKernel thresholderKernel;

    public BallDetector(BallDetectorKernel ballDetectorKernel, ThresholderKernel thresholderKernel) {
        this.ballDetectorKernel = ballDetectorKernel;
        this.thresholderKernel = thresholderKernel;
    }

    public Collection<Blob> findBalls(ColoredImage hsvImage) {
        GrayscaleImage certaintyMap = ballDetectorKernel.generateCertaintyMap(hsvImage);
        BinaryImage maxCertainty = thresholderKernel.threshold(certaintyMap, 205, 255);
        return findBalls(certaintyMap, maxCertainty);
    }

    private Collection<Blob> findBalls(GrayscaleImage certaintyMap, BinaryImage maxCertainty) {
        Collection<Blob> balls = new ArrayList<>();
        VisitedMap visitedMap = new VisitedMap(certaintyMap.getWidth(), certaintyMap.getHeight());
        int visitedMapPos = visitedMap.getStartIndex();
        int maxCertaintyPos = maxCertainty.getStartIndex();
        for(int y = 0; y < visitedMap.getHeight(); y++) {
            for(int x = 0; x < visitedMap.getWidth(); x++, visitedMapPos++, maxCertaintyPos++) {
                if(!maxCertainty.getData()[maxCertaintyPos] || visitedMap.getVisited()[visitedMapPos]) {
                    continue;
                }
                Blob ball = findBall(visitedMapPos, visitedMap, maxCertainty);
                if(ball != null) {
                    balls.add(ball);
                }
            }
            visitedMapPos += visitedMap.getStride() - visitedMap.getWidth();
            maxCertaintyPos += maxCertainty.getStride() - visitedMap.getWidth();
        }
        return balls;
    }

    private Blob findBall(int visitedMapStartPos, VisitedMap visitedMap, BinaryImage maxCertainty) {
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
            if(!maxCertainty.getData()[x + y * maxCertainty.getStride()]) {
                continue;
            }
            visitedMap.getVisited()[visitedMapPos] = true;
            count++;
            sumX += x;
            sumY += y;
            int[] visitedMapMoves = new int[] {-1, 1, -visitedMap.getStride(), visitedMap.getStride()};
            int[] maxCertaintyMoves = new int[] {-1, 1, -maxCertainty.getStride(), maxCertainty.getStride()};
            for(int i = 0; i < 4; i++) {
                int newVisitedMapPos = visitedMapPos + visitedMapMoves[i];
                int newMaxCertaintyPos = x + y * maxCertainty.getStride() + maxCertaintyMoves[i];
                if(!visitedMap.getVisited()[newVisitedMapPos] && maxCertainty.getData()[newMaxCertaintyPos]) {
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
