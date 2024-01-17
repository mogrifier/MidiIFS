package com.erichizdepski.ifs;
import javax.sound.midi.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
public class MidiHelp {


    /**
     * This creates a Sequence and a Track. The track is configured and returned.
     * @param fileName
     * @return
     * @throws InvalidMidiDataException
     */
    public static Sequence getSequence(String fileName, int[][] midiData) {

        // Create a new sequence with 960 tick per quarter note and 1 track
        Sequence sequence = null;
        // Create a new track
        Track track = null;

        try {
            sequence = new Sequence(Sequence.PPQ, 960, 1);
            track = sequence.createTrack();

            // Set the tempo of the track to 120 BPM (beats per minute)
            // microseconds per quarter note. OMG. last three bytes are 500,000 in hex bytes
            byte[] data = new byte[]{0x51, 0x03, 0x07, (byte)160, 0x20};
            //byte wtf = 0xa6 is not LEGAL- some UTF problem I think. so (byte)160.
            MidiMessage tempoMsg = new MetaMessage(0x51, data, data.length);
            //0x51 = set tempo. see https://mido.readthedocs.io/en/latest/meta_message_types.html
            MidiEvent tempoEvent = new MidiEvent(tempoMsg, 0);
            //adds event at time tick = 0
            track.add(tempoEvent);

            //loop through datasets to add midi events
            int startTick = 0;
            int duration = 0;

            for (int i = 0; i < midiData.length; i++) {
                // Add a Note On event to the track for C4 (MIDI note number 60) with velocity 64 at tick 0
                ShortMessage noteOn = new ShortMessage();
                noteOn.setMessage(ShortMessage.NOTE_ON, 0, midiData[i][0], 64);
                MidiEvent noteOnEvent = new MidiEvent(noteOn, startTick);
                track.add(noteOnEvent);

                duration = midiData[i][1];

                // Add a Note Off event to the track for the same note with velocity 0 at tick 96 (equivalent to a quarter note)
                ShortMessage noteOff = new ShortMessage();
                noteOff.setMessage(ShortMessage.NOTE_OFF, 0,  midiData[i][0], 0);
                MidiEvent noteOffEvent = new MidiEvent(noteOff, startTick + duration);
                track.add(noteOffEvent);

                startTick = startTick + duration;
            }



        }
        catch (InvalidMidiDataException e)
        {
            e.printStackTrace();
        }

        return sequence;
    }

    /**
     * Demonstrates how to write a MIDI file using code.
     */
    public static void writeMidiFile(Sequence sequence) {
        try (OutputStream stream =  new FileOutputStream(new File("./sample.mid"))) {

            //write the data to the console as a check.
            //MidiSystem.write(sequence, 1, System.out);

            // Write the sequence to a MIDI file named "output.mid". type 0 is not allowed by javax MIDI. Using type 1.
            MidiSystem.write(sequence, 1, stream);
            stream.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }




}
