package com.example;

public class App 
{
    public static void main( String[] args ) {
        System.out.print(args.length);
        System.out.print(" args:");
        for (String s: args) {
            System.out.printf(" <%s>", s);
        }
        System.out.println();
    }
}
