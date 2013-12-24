PrereqVis
=========

Course Prerequisite Visualizer

Made for displaying course prerequisites at Georgia Tech as a 2D directed graph. 

Input graph data sets are stored by subject in the gm.nodeode.data resource folder,
(the files with the format data_*.txt), and are read in by the NodeIO class. The
data files themselves were generated with a Python script reading data from OSCAR,
Georgia Tech's online course catalogue.

Currently, the user interface for the program is abysmal. It generates a graph based
on a data file chosen by a hard-coded string in the main gm.nodeode.NodeOde.java file,
which it then displays in a button you can click on to save it to your hard drive.
This is terrible for a variety of reasons, and I plan to change it to something less
awful in the future. In the mean time, if you want to play with it yourself, you can
change the hard-coded value.

Algorithms to lay out the nodes were developed based off of a paper published by
Bell Laboratories you can read here: http://www.graphviz.org/Documentation/TSE93.pdf
