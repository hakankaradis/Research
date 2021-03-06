#! /usr/bin/perl

#makes things work when run without install
use lib qw( /var/work/hkaradi1/gInstall/rrdtool/lib/perl );

use RRDs;

my $start=time;
my $rrd="randome.rrd";
my $name = $0;
$name =~ s/.*\///g;
$name =~ s/\.pl.*//g;

RRDs::create ($rrd, "--start",$start-1, "--step",300,
	      "DS:a:GAUGE:600:U:U",
	      "DS:b:GAUGE:600:U:U",
	      "RRA:AVERAGE:0.5:1:300",
	      "RRA:MIN:0.5:12:300",
	      "RRA:MAX:0.5:12:300",
);

my $ERROR = RRDs::error;
die "$0: unable to create `$rrd': $ERROR\n" if $ERROR;

# dropt some data into the rrd
my $t;
for ($t=$start; $t<$start+300*300; $t+=300){
  RRDs::update $rrd, "$t:".(sin($t/3000)*50+50).":".(sin($t/2500)*50+50);
  if ($ERROR = RRDs::error) {
    die "$0: unable to update `$rrd': $ERROR\n";
  }
}

my $c1="f57912a0";
my $c2="2a79e9a0";
my $w=300;
my $h=140;

RRDs::graph "$name-L.png",
  "--title", "2 LINES", 
  "--start", "now",
  "--end", "start+15h",
  "--lower-limit=0",
  "--interlace", 
  "--imgformat","PNG",
  "--width=$w",
  "--height=$h",
  "DEF:a=$rrd:a:AVERAGE",
  "DEF:b=$rrd:b:AVERAGE",
  "LINE1:a#$c1:Value A",
  "LINE3:b#$c2:Value B",
;

RRDs::graph "$name-A.png",
  "--title", "LINE and AREA", 
  "--start", "now",
  "--end", "start+15h",
  "--lower-limit=0",
  "--interlace", 
  "--imgformat","PNG",
  "--width=$w",
  "--height=$h",
  "DEF:a=$rrd:a:AVERAGE",
  "DEF:b=$rrd:b:AVERAGE",
  "AREA:a#$c1:Value A",
  "LINE2:b#$c2:Value B",
;

RRDs::graph "$name-S.png",
  "--title", "STACKED AREAS", 
  "--start", "now",
  "--end", "start+15h",
  "--lower-limit=0",
  "--interlace", 
  "--imgformat","PNG",
  "--width=$w",
  "--height=$h",
  "DEF:a=$rrd:a:AVERAGE",
  "DEF:b=$rrd:b:AVERAGE",
  "AREA:a#$c1:Value A",
  "STACK:b#$c2:Value B",
;


RRDs::graph "$name-M.png",
  "--title", "RPN Magic", 
  "--start", "now",
  "--end", "start+15h",
  "--lower-limit=0",
  "--interlace", 
  "--imgformat","PNG",
  "--width=$w",
  "--height=$h",
  "DEF:a=$rrd:a:AVERAGE",
  "DEF:b=$rrd:b:AVERAGE",
  "CDEF:alpha=TIME,3600,%,1800,LT,a,UNKN,IF",
  "CDEF:beta=TIME,3600,%,1800,GE,b,UNKN,IF",
  "AREA:alpha#$c1:Value A",
  "LINE1:a#$c1",
  "AREA:beta#$c2:Value B",
  "LINE1:b#$c2",
;

RRDs::graph "$name-sample.png",
  "--title", "Sample", 
  "--start", "now",
  "--end", "start+15h",
  "--lower-limit=0",
  "--interlace", 
  "--imgformat","PNG",
  "--width=600",
  "--height=50",
  "DEF:a=$rrd:a:AVERAGE",
  "DEF:b=$rrd:a:MAX",
  "AREA:a#00ff00:Incoming",
  "LINE1:b#ff0000:Max Incoming",
;

if ($ERROR = RRDs::error) {
  die "ERROR: $ERROR\n";
};

print "This script has created $name.png in the current directory\n";
print "This demonstrates the use of the TIME and % RPN operators\n";
