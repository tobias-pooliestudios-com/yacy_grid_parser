#!/bin/bash
adduser --disabled-password --gecos '' r
adduser r sudo
echo '%sudo ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers
cd /yacy_grid_parser
sleep 5s;
gradle run
