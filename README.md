****Movie Ticket Booking System – Java Console Application

This project is a Java-based console application that allows users to book movie tickets, view seat maps, cancel bookings, and check their reservations. All bookings are saved to a file, so they remain even after the program is closed

**** Features:
1)View list of available movies
2)View list of available shows
3)View seat map for any show
4)Book multiple seats
5)Unique Booking ID generation
6)Simulated payment gateway
7)Cancel bookings
8)View all bookings by username
9)Persistent storage using bookings.dat

**** Technologies Used :
# java 8+
# OOP Concept
# Collections framework
# Serialization
# LocalDateTime API

**** How to Compile & Run
1) Compile
javac MovieBookingSystem.java

2) Run
java MovieBookingSystem

**** Files Included

MovieBookingSystem.java -> Full application source code

README.md -> Documentation

bookings.dat -> Auto-created when bookings are made (no need to upload)

**** Project Description :
The system preloads sample movies and shows.
Each show has a seat map (Rows A–E, Columns 1–8 → 40 seats).
Users can book or cancel seats, and bookings are saved in a serialized file.

**** How Booking Works :
1)Choose a show
2)View available seats
3)Select seat IDs (e.g., A1, A2)
4)Simulated payment
5)Receive a unique Booking ID
