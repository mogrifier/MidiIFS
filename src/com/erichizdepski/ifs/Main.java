package com.erichizdepski.ifs;

public class Main {

    public static void main(String[] args){

        Generator g = new Generator(1000);
        g.setup();
        g.compute_music();
    }
}
