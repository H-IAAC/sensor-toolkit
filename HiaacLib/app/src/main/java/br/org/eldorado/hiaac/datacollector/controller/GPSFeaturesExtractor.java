package br.org.eldorado.hiaac.datacollector.controller;

import android.location.Location;

import java.util.List;

import br.org.eldorado.hiaac.datacollector.data.LabeledData;
import br.org.eldorado.hiaac.datacollector.model.ExtraSensoryData;
import br.org.eldorado.sensoragent.model.GPS;
import br.org.eldorado.sensoragent.model.SensorBase;

public class GPSFeaturesExtractor {

    private final ExtraSensoryData esData;
    public GPSFeaturesExtractor(ExtraSensoryData eS) {
        this.esData = eS;
    }

    public void computeFeatures(List<LabeledData> locations) {
        if (locations == null || locations.isEmpty()) {
            addFeature(ExtraSensoryData.FeatureName.LOCATION_NUM_VALID_UPDATES, 0.0);
            return;
        }

        addFeature(ExtraSensoryData.FeatureName.LOCATION_NUM_VALID_UPDATES, (double) locations.size());

        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        double minAlt = Double.MAX_VALUE, maxAlt = -Double.MAX_VALUE;
        double minSpeed = Double.MAX_VALUE, maxSpeed = -Double.MAX_VALUE;
        double bestHAcc = Double.MAX_VALUE;
        double bestVAcc = Double.MAX_VALUE;

        GPS loc = null;
        for (LabeledData ld : locations) {
            loc = (GPS)ld.getSensor();
            minLat = Math.min(minLat, loc.getLatitude());
            maxLat = Math.max(maxLat, loc.getLatitude());
            minLon = Math.min(minLon, loc.getLongitude());
            maxLon = Math.max(maxLon, loc.getLongitude());
            if (loc.hasAltitude()) {
                minAlt = Math.min(minAlt, loc.getAltitude());
                maxAlt = Math.max(maxAlt, loc.getAltitude());
            }
            if (loc.hasSpeed()) {
                minSpeed = Math.min(minSpeed, loc.getSpeed());
                maxSpeed = Math.max(maxSpeed, loc.getSpeed());
            }
            if (loc.hasAccuracy()) {
                bestHAcc = Math.min(bestHAcc, loc.getHorizontalPrecision());
            }
            if (loc.hasVerticalAccuracy()) {
                bestVAcc = Math.min(bestVAcc, loc.getVerticalPrecision());
            }
        }

        addFeature(ExtraSensoryData.FeatureName.LOCATION_LOG_LATITUDE_RANGE, logSafe(maxLat - minLat));
        addFeature(ExtraSensoryData.FeatureName.LOCATION_LOG_LONGITUDE_RANGE, logSafe(maxLon - minLon));
        addFeature(ExtraSensoryData.FeatureName.LOCATION_MIN_ALTITUDE, safeVal(minAlt));
        addFeature(ExtraSensoryData.FeatureName.LOCATION_MAX_ALTITUDE, safeVal(maxAlt));
        addFeature(ExtraSensoryData.FeatureName.LOCATION_MIN_SPEED, safeVal(minSpeed));
        addFeature(ExtraSensoryData.FeatureName.LOCATION_MAX_SPEED, safeVal(maxSpeed));
        addFeature(ExtraSensoryData.FeatureName.LOCATION_BEST_HORIZONTAL_ACCURACY, safeVal(bestHAcc));
        addFeature(ExtraSensoryData.FeatureName.LOCATION_BEST_VERTICAL_ACCURACY, safeVal(bestVAcc));

        // compute diameter (máxima distância entre pontos)
        double diameter = computeDiameter(locations);
        addFeature(ExtraSensoryData.FeatureName.LOCATION_DIAMETER, diameter);
        addFeature(ExtraSensoryData.FeatureName.LOCATION_LOG_DIAMETER, logSafe(diameter));

        // quick features
        double stdLat = std(locations, true);
        double stdLon = std(locations, false);
        addFeature(ExtraSensoryData.FeatureName.LOCATION_QUICK_FEATURES_STD_LAT, stdLat);
        addFeature(ExtraSensoryData.FeatureName.LOCATION_QUICK_FEATURES_STD_LONG, stdLon);

        addFeature(ExtraSensoryData.FeatureName.LOCATION_QUICK_FEATURES_LAT_CHANGE,
                ((GPS)locations.get(locations.size()-1).getSensor()).getLatitude() - ((GPS)locations.get(0).getSensor()).getLatitude());
        addFeature(ExtraSensoryData.FeatureName.LOCATION_QUICK_FEATURES_LONG_CHANGE,
                ((GPS)locations.get(locations.size()-1).getSensor()).getLongitude() - ((GPS)locations.get(0).getSensor()).getLongitude());

        addFeature(ExtraSensoryData.FeatureName.LOCATION_QUICK_FEATURES_MEAN_ABS_LAT_DERIV, meanAbsDerivative(locations, true));
        addFeature(ExtraSensoryData.FeatureName.LOCATION_QUICK_FEATURES_MEAN_ABS_LONG_DERIV, meanAbsDerivative(locations, false));
    }

    private static double computeDiameter(List<LabeledData> locs) {
        double maxDist = 0;
        Location loc1 = null;
        Location loc2 = null;
        for (int i = 0; i < locs.size(); i++) {
            loc1 = new Location("loc1");
            loc1.setLatitude(((GPS)locs.get(i).getSensor()).getLatitude());
            loc1.setLongitude(((GPS)locs.get(i).getSensor()).getLongitude());
            if (((GPS)locs.get(i).getSensor()).hasAltitude()) {
                loc1.setAltitude(((GPS)locs.get(i).getSensor()).getAltitude());
            }
            for (int j = i + 1; j < locs.size(); j++) {
                loc2 = new Location("loc2");
                loc2.setLatitude(((GPS)locs.get(j).getSensor()).getLatitude());
                loc2.setLongitude(((GPS)locs.get(j).getSensor()).getLongitude());
                if (((GPS)locs.get(j).getSensor()).hasAltitude()) {
                    loc2.setAltitude(((GPS)locs.get(j).getSensor()).getAltitude());
                }
                maxDist = Math.max(maxDist, loc1.distanceTo(loc2));
            }
        }
        return maxDist;
    }

    private static double std(List<LabeledData> locs, boolean isLat) {
        double mean = 0;
        for (LabeledData l : locs) {
            mean += isLat ? ((GPS)l.getSensor()).getLatitude() : ((GPS)l.getSensor()).getLongitude();
        }
        mean /= locs.size();

        double sumSq = 0;
        for (LabeledData l : locs) {
            double v = isLat ? ((GPS)l.getSensor()).getLatitude() : ((GPS)l.getSensor()).getLongitude();
            sumSq += Math.pow(v - mean, 2);
        }
        return Math.sqrt(sumSq / locs.size());
    }

    private static double meanAbsDerivative(List<LabeledData> locs, boolean isLat) {
        if (locs.size() < 2) return 0;
        double sum = 0;
        GPS prevLoc = null;
        GPS currLoc = null;
        for (int i = 1; i < locs.size(); i++) {
            prevLoc = (GPS)locs.get(i - 1).getSensor();
            currLoc = (GPS)locs.get(i).getSensor();
            double prev = isLat ? prevLoc.getLatitude() : prevLoc.getLongitude();
            double cur = isLat ? currLoc.getLatitude() : currLoc.getLongitude();
            sum += Math.abs(cur - prev);
        }
        return sum / (locs.size() - 1);
    }

    private static double logSafe(double val) {
        return (val <= 0) ? 0 : Math.log10(val);
    }

    private static double safeVal(double val) {
        return Double.isFinite(val) ? val : 0;
    }

    private void addFeature(ExtraSensoryData.FeatureName name, double value) {
        esData.addFeature(name, SensorBase.TYPE_GPS, value);
    }
}
