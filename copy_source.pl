#!/usr/bin/perl

use strict;
use warnings;

use File::Find;
use File::Path;
use File::Copy;
use File::Basename;
use Archive::Tar;

# This Perl script copies source codes and resources such as image files back
# and forth between the local project directory and the git repository.
# Currently this is just a quick-and-dirty approach and have only been tested
# within a test environment.
#
# Written by Naoki Mizuno

# TODO: Read config file
# TODO: Backup feature

my $PACKAGE_NAME = "com.me.myverilogTown";

# "foobar" if you have "foobar-android" or "foobar-desktop" directories
my $NAME = "verilogTown";
my @postpositions = ( "", "-desktop", "-android", "-html" );
my @subdirectories = qw( src assets res );
my @extensions = qw( java png );

# The path (relative or absolute) right above the projects
# VERILOG_TOWN <-- this path!
#   |
#   +-- verilogTown
#   |
#   +-- verilogTown-android
#   |
#   +-- verilogTown-desktop
#   |
#   +-- verilogTown-html

# Check for validity of arguments
unless (@ARGV == 2) {
    print_help();
    exit;
}

my $from = shift;
my $to = shift;
# Make the path absolute if relative
$from = "$ENV{PWD}/$from" unless $from =~ /^\//;
$to = "$ENV{PWD}/$to" unless $to =~ /^\//;

# Die if source directory can't be found
die "Can't find source directory $from" unless -d $from;

foreach my $postposition (@postpositions) {
    foreach my $subdirectory (@subdirectories) {
        # e.g. verilogTown-android/src/com/me/myverilogTown
        my $dir_name = "";
        if ($subdirectory eq "src") {
            $dir_name = $NAME . $postposition . "/$subdirectory/" . ($PACKAGE_NAME =~ s/\./\//gr);
        }
        else {
            $dir_name = $NAME . $postposition . "/$subdirectory/";
        }

        next unless -d "$from/$dir_name";

        # Create "to" if it doesn't exist
        mkpath "$to/$dir_name" unless -d "$to/$dir_name";

        find(sub { copy_file($dir_name) }, "$from/$dir_name");
    }
}

sub copy_file {
    my $file_name = $_;
    my $dir_name = shift;

    # Return if the file name doesn't end with the extension
    return unless $file_name =~ m/\.@{[ join "|", @extensions ]}$/;

    # TODO: Explain how this works
    my $diff = $File::Find::name =~ s/$from\/$dir_name//r;

    my $from_path = "$from/$dir_name/$diff";
    my $to_path = "$to/$dir_name/$diff";

    mkpath dirname $to_path unless -d dirname $to_path;

    if (copy $from_path, $to_path) {
        print "Copied $from_path\n\t=> $to_path\n";
    }
    else {
        die "Couldn't copy $from_path: $!";
    }
}

# Returns the file name for the tar file in the format of %Y%m%d%H%M%S.tar.gz
sub get_archive_file_name {
    my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime();

    return sprintf "%4d%02d%02d%02d%02d%02d.tar.gz",
        1900 + $year, 1 + $mon, $mday, $hour, $min, $sec;
}

sub print_help {
    print <<EOF;
Usage: @{[basename $0]} [from dir] [to dir]
    The path can be either absolute or relative.
    Specify the the parent directory of the directories ending with
    "-desktop" or "-android" that were generated by the libgdx setup UI.
    For examlpe, for the following directory tree,

    ...
    |
    +-- VERILOG_TOWN
    |       |
    |       +-- verilogTown
    |       |   |
    |       |   +-- src
    |       |   +-- ...
    |       +-- verilogTown-desktop
    |       |
    |       +-- verilogTown-android
    |       +-- ...
    +-- ...

    "VERILOG_TOWN" must be the argument.
EOF
}
