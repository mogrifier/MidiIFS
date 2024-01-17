package com.erichizdepski.ifs;

import javax.sound.midi.*;
import java.io.File;

/*
play tones. Arduino MEGA 1280.

This is creating really nice and usable sync-like tones. All with with output voice.
Very worthwhile to put separate analog controls on the variables for freq, vibratorate, maxMod
and maybe loop delay.
This is a continuous player- run through FX for sure!
duration should be less than loopdelay

*/
public class Generator {

    static final int FREQ = 500;
    static final float TWELFTHROOT = 1.05946f;
    static final float LOWA = 27.5f;
    static final int MAX_NOTE = 88;
    static final int MINIUMUM_DURATION = 100;
    //these are not constant and will be changeable with sensor inputs

    int iterations = 0;
    int index = 0;
    //other global variables- minimize use
    int arpeggiatorDuration = 500;  //default to 120BPM

    //MIDI stuff
    static final int NOTEOFF = 128;
    static final int NOTEON = 144;

    byte[] kIndex;

    //arrays for holding IFS matrix
    float[] a = new float[4];
    float[] b = new float[4];
    float[] c = new float[4];
    float[] d = new float[4];
    float[] e = new float[4];
    float[] f = new float[4];

    public Generator(int iterations) {
            /*
    Need buffers to store data for playback of arpeggio patterns from either a saved random
    pattern or a set of notes played in via the keyboard. byte uses half memory of int and works fine.
    */
        this.iterations = iterations;
        kIndex = new byte[iterations];
    }

    void setup() {
        // put your setup code here, to run once:

        //initialize kIndex
        fill_kIndex();

        //initial IFS matrix data for a fern
        a[0] = 0;
        a[1] = 0.85f;
        a[2] = 0.2f;
        a[3] = -0.15f;

        b[0] = 0;
        b[1] = 0.04f;
        b[2] = -0.26f;
        b[3] = 0.28f;

        c[0] = 0;
        c[1] = -0.04f;
        c[2] = 0.23f;
        c[3] = 0.26f;

        d[0] = 0.16f;
        d[1] = 0.85f;
        d[2] = 0.22f;
        d[3] = 0.44f;

        //made these non-zero since they were driving x to zero, which is boring
        e[0] = 0.1f;
        e[1] = 0.2f;
        e[2] = 0.05f;
        e[3] = 0.3f;

        f[0] = 0;
        f[1] = 1.6f;
        f[2] = 1.6f;
        f[3] = 0.44f;

    }


    /*
    Get the next pitch and duration from the IFS code. Just compute all as needed. This use two knobs to determine
    the start and end point fo the pattern to play with the entire pattern. If the knbos values are bad (end < start)
    then an end value of start + 10 is used. Chaos controls the range of pitch and duration values. Low value of chaos (order) means
    pitch and duration will not have as extreme jumps since the range is tighter. Max chaos equates to max range of
    pitch and duration.
    */
    void compute_music() {

        int[][] midiData = new int[this.iterations][2];

        //starting point for IFS code. Store past and present for iteration.
        float x = 0f;
        float y = 0f;
        float next_x = 0f;
        float next_y = 0f;

        for (int i = 0; i < iterations; i++) {

            byte k = (byte) get_chance();

              /* Note that not all matrix values are read for computation of next values. This prevents changes
              to values being included in the calculation must of the time, depending on how get_chance() is weighed.
              Array slot 2 is the highest probability, followed by 3.*/
            //pitch
            next_x = a[k] * x + b[k] * y + e[k];
            //duration
            next_y = c[k] * x + d[k] * y + f[k];
            x = next_x;
            y = next_y;


            //need to watch for overflow and nan conditions. seems odd, but does happen
            if (Float.isNaN(x)) {
                x = 0.1f;
            }
            if (Float.isNaN(y)) {
                y = 0.3f;
            }

            //the next note to play is next_x with a duration of next_y

            //scale values so in bounds and make sense for pitch frequency and duration in milliseconds
            int scale_x = (int) (Math.abs(x) * 100);
            if (scale_x > 100) {
                scale_x = 100;
            }

          /* apply Chaos control to mapping function applied to sensor. The top note can be reduced by
          a 1.5 octaves while maintaining a good distriubtion. This works in conjuction with duration change.
          Order means lower high pitch, and higher minimum duration.
          */
            if (y < 0)
            {
                y = y * -1f;
            }


            //do I need * duration scale??
            int scale_y = map((int)y, 0, 10, MINIUMUM_DURATION, 1375);
            //constrain the piano key range to one the arduino can play and also not too high since unpleasant
            int piano_key = map(scale_x, 0, 100, 25, MAX_NOTE);

            //add data to integer arrays of Midi data
            midiData[i][0] = piano_key;
            midiData[i][1] = scale_y;

        }

        //write the midi file
        Sequence sequence = MidiHelp.getSequence("ifs.mid", midiData);
        MidiHelp.writeMidiFile(sequence);
    }


    /*
    Choose array indices based on a hard-coded probability distribution.
    */
    int get_chance() {
        //random off- just return next value from kIndex (up to END_ITERATIONs) or start over at start (which can be zero)
        if (index == iterations) {
            index = 0;
        }
        //index will either be 0 or the next value. This logic keeps it in range.
        return kIndex[index++];
    }


    /*
    Convert the piano key position (1 to 88) to the corresponding frequency
    */
    int get_freq(int key) {
        //adjust for difference between piano and midi numbering
        key = key + 4;
        int octave = (int)(key / 12);
        int note = (key % 12) - 1;
        float freq = (float)(LOWA * Math.pow(2, octave) * Math.pow(TWELFTHROOT, note));
        //round to nearest whole value
        return (int)(freq + 0.5);
    }



    void fill_kIndex() {
        //fill with random values. This only affects pitch, by design.
        for (int i = 0; i < iterations; i++) {
            kIndex[i] = (byte)getIFSProbabilty();
        }
    }

    /*
    This gets a k value (array index) based on probability rules that are part of the IFS.
    */
    int getIFSProbabilty() {
        float r = (float)(Math.random() * 100 / 100);
        if (r <= 0.1)
            return 0;
        if (r <= 0.2)
            return 1;
        if (r <= 0.4)
            return 2;
        else
            return 3;
    }




    void triggerGlitch() {
        //base this on a callback on a keystroke
        return;
    }


    int map(int value, int fromLow, int fromHigh, int toLow, int toHigh) {
        // Ensure the value is within the specified range
        value = Math.min(Math.max(value, fromLow), fromHigh);

        // Map the value from the input range to the output range
        return (int) (toLow + (long) (value - fromLow) * (toHigh - toLow) / (fromHigh - fromLow));
    }

}
