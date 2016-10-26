package cs.umass.edu.myactivitiestoolkit.steps;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import java.util.ArrayList;
import java.util.StringTokenizer;

import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.processing.Filter;

/**
 * This class is responsible for detecting steps from the accelerometer sensor.
 * All {@link OnStepListener step listeners} that have been registered will
 * be notified when a step is detected.
 */
public class StepDetector implements SensorEventListener {
    /** Used for debugging purposes. */
    @SuppressWarnings("unused")
    private static final String TAG = StepDetector.class.getName();

    /** Maintains the set of listeners registered to handle step events. **/
    private ArrayList<OnStepListener> mStepListeners;

    /**
     * The number of steps taken.
     */
    private int stepCount;

    private final String  NATURAL = "natural";
    private final String INCREASING = "increasing";
    private final String DECREASING = "decreasing";
    private int dataCounter = 0;
    private int pendingZeroDataCounter = 0;
    private double startValue;
    private double startTime;
    String status = NATURAL;

    public StepDetector(){
        mStepListeners = new ArrayList<>();
        stepCount = 0;
    }

    /**
     * Registers a step listener for handling step events.
     * @param stepListener defines how step events are handled.
     */
    public void registerOnStepListener(final OnStepListener stepListener){
        mStepListeners.add(stepListener);
    }

    /**
     * Unregisters the specified step listener.
     * @param stepListener the listener to be unregistered. It must already be registered.
     */
    public void unregisterOnStepListener(final OnStepListener stepListener){
        mStepListeners.remove(stepListener);
    }

    /**
     * Unregisters all step listeners.
     */
    public void unregisterOnStepListeners(){
        mStepListeners.clear();
    }

    /**
     * Here is where you will receive accelerometer readings, buffer them if necessary
     * and run your step detection algorithm. When a step is detected, call
     * {@link #onStepDetected(long, float[])} to notify all listeners.
     *
     * Recall that human steps tend to take anywhere between 0.5 and 2 seconds.
     *
     * @param event sensor reading
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        Filter bufferingFilter = new Filter(3.0);
        Filter smoothFilter = new Filter(1);
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            double doublesFilterValue[] = bufferingFilter.getFilteredValues(event.values);
            double[] filterValues = smoothFilter.getFilteredValues(convertToFloatArray(doublesFilterValue));
            //TODO: Detect steps! Call onStepDetected(...) when a step is detected.
            double vectorProducts = Math.pow(filterValues[0],2)+Math.pow(filterValues[1],2)+
                    Math.pow(filterValues[2],2);
            double vectorSqrt = Math.sqrt(vectorProducts);
            if(dataCounter == 0) {
                startValue = vectorSqrt;
                startTime = (double) event.timestamp;
            }
            switch (status) {
                case NATURAL: {
                    if(dataCounter==15){
                        double endTime = ((double) event.timestamp);
                        int slope = slopeTimesTen(startTime,endTime,startValue,vectorSqrt);
                        if(slope>0){
                            status = INCREASING;
                        }else if(slope<0) {
                            status = DECREASING;
                        }else {
                            status = NATURAL;
                        }
                        dataCounter = 0;
                    }else {
                        dataCounter += 1;
                    }
                    break;
                }
                case INCREASING: {
                    if(dataCounter==15){
                        double endTime = ((double) event.timestamp);
                        int slope = slopeTimesTen(startTime,endTime,startValue,vectorSqrt);
                        if(slope>0){
                            pendingZeroDataCounter = 0;
                            status = INCREASING;
                        }else if(slope<0) {
                            pendingZeroDataCounter = 0;
//                            onStepDetected((long) ((double) event.timestamp / Constants.TIMESTAMPS.NANOSECONDS_PER_MILLISECOND),event.values);
                            status = DECREASING;
                        }else {
                            pendingZeroDataCounter += 1;
                        }
                        dataCounter = 0;
                    }else {
                        dataCounter += 1;
                    }
                    break;
                }
                case DECREASING: {
                    if(dataCounter==15){
                        double endTime = ((double) event.timestamp);
                        int slope = slopeTimesTen(startTime,endTime,startValue,vectorSqrt);
                        if(slope>0){
                            pendingZeroDataCounter = 0;
                            onStepDetected((long) ((double) event.timestamp / Constants.TIMESTAMPS.NANOSECONDS_PER_MILLISECOND),event.values);
                            status = INCREASING;
                        }else if(slope<0) {
                            pendingZeroDataCounter = 0;
                            status = DECREASING;
                        }else {
                            pendingZeroDataCounter += 1;
                        }
                        dataCounter = 0;
                    }else {
                        dataCounter += 1;
                    }
                    break;
                }
            }
                if (pendingZeroDataCounter == 10) {
                    status = NATURAL;
                    dataCounter = 0;
                    pendingZeroDataCounter = 0;
                }
        }
    }

    private int slopeTimesTen(double startTime, double endTime, double sValue, double eValue) {
//        Log.w("My startValue is", ""+sValue);
//        Log.w("My endValue is ", ""+eValue);
//        Log.w("My startTime is", ""+startTime);
//        Log.w("My endTime is ", ""+endTime);
        double slope = ((eValue-sValue)/(endTime-startTime))*Math.pow(10,9)*3;
        return (int)slope;
    }

    private float[] convertToFloatArray(double[] doubleArray) {
        float[] floatArray = new float[doubleArray.length];
        for (int i = 0 ; i < doubleArray.length; i++)
        {
            floatArray[i] = (float) doubleArray[i];
        }
        return  floatArray;
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // do nothing
    }

    /**
     * This method is called when a step is detected. It updates the current step count,
     * notifies all listeners that a step has occurred and also notifies all listeners
     * of the current step count.
     */
    private void onStepDetected(long timestamp, float[] values){
        stepCount++;
        for (OnStepListener stepListener : mStepListeners){
            stepListener.onStepDetected(timestamp, values);
            stepListener.onStepCountUpdated(stepCount);
        }
    }
}
