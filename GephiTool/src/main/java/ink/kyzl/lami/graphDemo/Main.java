/*
Licensed under MIT licence:

Copyright (C) 2019 Jiří Kyzlink <jkyzlink@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package ink.kyzl.lami.graphDemo;

import java.io.File;

public class Main {

    public static void main(String[] args) throws Exception {
        LamiDemo lamiDemo = new LamiDemo();
        if(args.length == 2){
            if(!new File(args[0]).isFile()){
                System.err.println("Input file does not exists.");
                System.exit(1);
            }    
            if(!new File(args[1]).exists())
                System.out.println("Output file will be overwritten");
            
            lamiDemo.script(args[0], args[1]);
            System.exit(0);
        }           
        
        System.err.println(
                "Invalid arguments, specify input and output file names.\n"
                + "First argument is input file,\n"
                + "second argument is output file (PDF)"
        );
        
        System.exit(2);
    }
}
