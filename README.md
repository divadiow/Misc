# Hotspot Devices

A standalone Android app that discovers IPv4 devices connected to the phone's hotspot without root and without depending on Termux.

It detects likely hotspot interfaces, derives the subnet, allows manual CIDR entry, scans up to 1,024 addresses using ordinary unprivileged socket probes, lists responsive IP addresses and common TCP services, supports auto-refresh while open, and shares a text report.

Modern Android blocks third-party apps from reading hotspot DHCP leases and ARP/neighbour tables. Consequently, this app cannot reliably obtain MAC addresses or client names. It discovers clients actively, similarly to an unprivileged `nmap -sn` scan.
