import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * MovieBookingSystem
 *
 * Single-file, runnable Java console application for booking movie tickets.
 *
 * Features:
 * - Sample movies and shows are preloaded.
 * - Each show has a seat map (rows A-E, columns 1-8 => 40 seats).
 * - View movies, view shows, view seat map, book seats, cancel booking, view your bookings.
 * - Bookings are assigned a unique booking ID.
 * - Simple simulated payment step.
 * - Bookings persist to file "bookings.dat" so they survive runs.
 *
 * To compile:
 *   javac MovieBookingSystem.java
 *
 * To run:
 *   java MovieBookingSystem
 */
public class MovieBookingSystem {

    /* ---------- Domain classes ---------- */

    static class Movie implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String id;
        private final String title;
        private final String language;
        private final int durationMinutes;
        private final String genre;

        public Movie(String id, String title, String language, int durationMinutes, String genre) {
            this.id = id;
            this.title = title;
            this.language = language;
            this.durationMinutes = durationMinutes;
            this.genre = genre;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getLanguage() { return language; }
        public int getDurationMinutes() { return durationMinutes; }
        public String getGenre() { return genre; }

        @Override
        public String toString() {
            return String.format("[%s] %s (%s, %d min, %s)", id, title, language, durationMinutes, genre);
        }
    }

    static class Show implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String id;
        private final Movie movie;
        private final LocalDateTime startTime;
        private final String screenName;
        private final Map<String, Seat> seats; // seatId -> Seat
        private final double pricePerSeat;

        public Show(String id, Movie movie, LocalDateTime startTime, String screenName, int rows, int cols, double pricePerSeat) {
            this.id = id;
            this.movie = movie;
            this.startTime = startTime;
            this.screenName = screenName;
            this.seats = new LinkedHashMap<>();
            this.pricePerSeat = pricePerSeat;
            initSeats(rows, cols);
        }

        private void initSeats(int rows, int cols) {
            // Rows labelled A, B, C...
            for (int r = 0; r < rows; r++) {
                char rowChar = (char) ('A' + r);
                for (int c = 1; c <= cols; c++) {
                    String seatId = String.format("%c%d", rowChar, c);
                    seats.put(seatId, new Seat(seatId));
                }
            }
        }

        public String getId() { return id; }
        public Movie getMovie() { return movie; }
        public LocalDateTime getStartTime() { return startTime; }
        public String getScreenName() { return screenName; }
        public Map<String, Seat> getSeats() { return seats; }
        public double getPricePerSeat() { return pricePerSeat; }

        public int availableSeatsCount() {
            int count = 0;
            for (Seat s : seats.values()) if (!s.isBooked()) count++;
            return count;
        }

        @Override
        public String toString() {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return String.format("[%s] %s - %s at %s | Screen: %s | Price: ₹%.2f | Available: %d",
                    id, movie.getTitle(), movie.getLanguage(), startTime.format(fmt),
                    screenName, pricePerSeat, availableSeatsCount());
        }

        public boolean isSeatAvailable(String seatId) {
            Seat s = seats.get(seatId);
            return s != null && !s.isBooked();
        }

        public boolean bookSeat(String seatId, String bookingId) {
            Seat s = seats.get(seatId);
            if (s == null) return false;
            synchronized (s) {
                if (s.isBooked()) return false;
                s.setBooked(true);
                s.setBookingId(bookingId);
                return true;
            }
        }

        public boolean cancelSeat(String seatId) {
            Seat s = seats.get(seatId);
            if (s == null) return false;
            synchronized (s) {
                if (!s.isBooked()) return false;
                s.setBooked(false);
                s.setBookingId(null);
                return true;
            }
        }

        public void printSeatMap() {
            // print rows and columns nicely
            // derive rows from seat ids
            TreeSet<String> keys = new TreeSet<>(seats.keySet());
            // find max cols
            int maxCol = 0;
            for (String k : keys) {
                String num = k.substring(1);
                int c = Integer.parseInt(num);
                if (c > maxCol) maxCol = c;
            }
            // group by row
            Map<Character, List<Seat>> byRow = new TreeMap<>();
            for (String id : keys) {
                char r = id.charAt(0);
                byRow.computeIfAbsent(r, rr -> new ArrayList<>()).add(seats.get(id));
            }
            System.out.println("Seat legend: [O] available  [X] booked");
            System.out.print("   ");
            for (int c = 1; c <= maxCol; c++) System.out.printf("%3d", c);
            System.out.println();
            for (Map.Entry<Character, List<Seat>> e : byRow.entrySet()) {
                System.out.print(e.getKey() + "  ");
                for (Seat s : e.getValue()) {
                    System.out.print(s.isBooked() ? " [X]" : " [O]");
                }
                System.out.println();
            }
        }
    }

    static class Seat implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String id;
        private boolean booked;
        private String bookingId; // Which booking holds this seat (if any)

        public Seat(String id) {
            this.id = id;
            this.booked = false;
            this.bookingId = null;
        }

        public String getId() { return id; }
        public boolean isBooked() { return booked; }
        public void setBooked(boolean booked) { this.booked = booked; }
        public String getBookingId() { return bookingId; }
        public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    }

    static class Booking implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String bookingId;
        private final String userName;
        private final String showId;
        private final List<String> seatIds;
        private final double amount;
        private final LocalDateTime bookedAt;

        public Booking(String bookingId, String userName, String showId, List<String> seatIds, double amount, LocalDateTime bookedAt) {
            this.bookingId = bookingId;
            this.userName = userName;
            this.showId = showId;
            this.seatIds = new ArrayList<>(seatIds);
            this.amount = amount;
            this.bookedAt = bookedAt;
        }

        public String getBookingId() { return bookingId; }
        public String getUserName() { return userName; }
        public String getShowId() { return showId; }
        public List<String> getSeatIds() { return Collections.unmodifiableList(seatIds); }
        public double getAmount() { return amount; }
        public LocalDateTime getBookedAt() { return bookedAt; }

        @Override
        public String toString() {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return String.format("BookingID: %s | User: %s | Show: %s | Seats: %s | Amount: ₹%.2f | Time: %s",
                    bookingId, userName, showId, seatIds, amount, bookedAt.format(fmt));
        }
    }

    /* ---------- System data & persistence ---------- */

    private final Map<String, Movie> movies = new LinkedHashMap<>();
    private final Map<String, Show> shows = new LinkedHashMap<>();
    private final Map<String, Booking> bookings = new LinkedHashMap<>();
    private final Scanner scanner = new Scanner(System.in);
    private final String BOOKINGS_FILE = "bookings.dat";

    public MovieBookingSystem() {
        loadSampleData();
        loadBookingsFromFile();
    }

    private void loadSampleData() {
        // create some movies
        Movie m1 = new Movie("M001", "The Timekeeper", "English", 130, "Sci-Fi");
        Movie m2 = new Movie("M002", "Dil Se Again", "Hindi", 150, "Romance/Drama");
        Movie m3 = new Movie("M003", "The Chef's Secret", "Hindi", 120, "Comedy");
        movies.put(m1.getId(), m1);
        movies.put(m2.getId(), m2);
        movies.put(m3.getId(), m3);

        // create shows
        // rows=5, cols=8 -> 40 seats (A1..E8)
        LocalDateTime now = LocalDateTime.now();
        Show s1 = new Show("S101", m1, now.plusDays(0).withHour(13).withMinute(30), "Screen 1", 5, 8, 200.0);
        Show s2 = new Show("S102", m1, now.plusDays(0).withHour(19).withMinute(0), "Screen 1", 5, 8, 250.0);
        Show s3 = new Show("S201", m2, now.plusDays(0).withHour(15).withMinute(0), "Screen 2", 5, 8, 220.0);
        Show s4 = new Show("S301", m3, now.plusDays(1).withHour(11).withMinute(0), "Screen 3", 5, 8, 150.0);

        shows.put(s1.getId(), s1);
        shows.put(s2.getId(), s2);
        shows.put(s3.getId(), s3);
        shows.put(s4.getId(), s4);
    }

    @SuppressWarnings("unchecked")
    private void loadBookingsFromFile() {
        File f = new File(BOOKINGS_FILE);
        if (!f.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            Object obj = ois.readObject();
            if (obj instanceof Map) {
                Map<String, Booking> loaded = (Map<String, Booking>) obj;
                bookings.putAll(loaded);
                // Reconcile seat bookings in shows
                for (Booking b : bookings.values()) {
                    Show sh = shows.get(b.getShowId());
                    if (sh != null) {
                        for (String seatId : b.getSeatIds()) {
                            sh.bookSeat(seatId, b.getBookingId());
                        }
                    }
                }
                System.out.println("Loaded " + bookings.size() + " bookings from disk.");
            }
        } catch (Exception e) {
            System.out.println("Warning: Couldn't load bookings file: " + e.getMessage());
        }
    }

    private void saveBookingsToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(BOOKINGS_FILE))) {
            oos.writeObject(bookings);
        } catch (Exception e) {
            System.out.println("Error saving bookings: " + e.getMessage());
        }
    }

    /* ---------- Console UI & operations ---------- */

    public void start() {
        System.out.println("Welcome to the Movie Ticket Booking System!");
        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = prompt("Enter choice");
            switch (choice) {
                case "1": listMovies(); break;
                case "2": listShows(); break;
                case "3": viewShowSeatMap(); break;
                case "4": bookSeats(); break;
                case "5": cancelBooking(); break;
                case "6": viewMyBookings(); break;
                case "7": saveBookingsToFile(); System.out.println("Bookings saved. Exiting..."); running = false; break;
                default: System.out.println("Unknown choice. Please choose 1-7."); break;
            }
        }
    }

    private void printMainMenu() {
        System.out.println("\nMain Menu");
        System.out.println("1. List movies");
        System.out.println("2. List shows");
        System.out.println("3. View seat map for a show");
        System.out.println("4. Book seats");
        System.out.println("5. Cancel booking");
        System.out.println("6. View my bookings");
        System.out.println("7. Save & Exit");
    }

    private void listMovies() {
        System.out.println("\nAvailable Movies:");
        for (Movie m : movies.values()) {
            System.out.println("  " + m);
        }
    }

    private void listShows() {
        System.out.println("\nAvailable Shows:");
        for (Show s : shows.values()) {
            System.out.println("  " + s);
        }
    }

    private void viewShowSeatMap() {
        String showId = prompt("Enter show id (e.g. S101) to view seat map");
        Show s = shows.get(showId);
        if (s == null) {
            System.out.println("Show id not found.");
            return;
        }
        System.out.println("\nShow: " + s);
        s.printSeatMap();
    }

    private void bookSeats() {
        String userName = prompt("Enter your name (for booking)");
        String showId = prompt("Enter show id to book (e.g. S101)");
        Show s = shows.get(showId);
        if (s == null) {
            System.out.println("Invalid show id.");
            return;
        }
        System.out.println("\nShow: " + s);
        s.printSeatMap();
        System.out.printf("Price per seat: ₹%.2f%n", s.getPricePerSeat());
        String seatsInput = prompt("Enter seat ids to book separated by commas (e.g. A1,A2)");
        List<String> seatIds = parseSeatList(seatsInput);
        if (seatIds.isEmpty()) {
            System.out.println("No seats entered.");
            return;
        }
        // Validate availability
        List<String> unavailable = new ArrayList<>();
        for (String seatId : seatIds) {
            if (!s.isSeatAvailable(seatId)) unavailable.add(seatId);
        }
        if (!unavailable.isEmpty()) {
            System.out.println("These seats are not available: " + unavailable);
            return;
        }
        double total = s.getPricePerSeat() * seatIds.size();
        System.out.printf("Total amount: ₹%.2f%n", total);
        String confirm = prompt("Proceed to payment? (y/n)");
        if (!confirm.equalsIgnoreCase("y")) {
            System.out.println("Booking cancelled by user.");
            return;
        }
        // Simulated payment
        boolean paid = simulatePayment(total);
        if (!paid) {
            System.out.println("Payment failed. Booking aborted.");
            return;
        }
        // Create booking
        String bookingId = generateBookingId();
        boolean allBooked = true;
        for (String seatId : seatIds) {
            boolean ok = s.bookSeat(seatId, bookingId);
            if (!ok) {
                allBooked = false;
                System.out.println("Failed to book seat: " + seatId);
            }
        }
        if (!allBooked) {
            System.out.println("Partial booking occurred; please contact support.");
        }
        Booking booking = new Booking(bookingId, userName, showId, seatIds, total, LocalDateTime.now());
        bookings.put(bookingId, booking);
        saveBookingsToFile(); // persist right away
        System.out.println("Booking successful! Your booking details:");
        System.out.println(booking);
    }

    private void cancelBooking() {
        String bookingId = prompt("Enter booking id to cancel (e.g. B-0001)");
        Booking b = bookings.get(bookingId);
        if (b == null) {
            System.out.println("Booking id not found.");
            return;
        }
        System.out.println("Booking found: " + b);
        String confirm = prompt("Confirm cancellation? Refund will be simulated. (y/n)");
        if (!confirm.equalsIgnoreCase("y")) {
            System.out.println("Cancellation aborted.");
            return;
        }
        Show s = shows.get(b.getShowId());
        if (s != null) {
            for (String seatId : b.getSeatIds()) {
                s.cancelSeat(seatId);
            }
        }
        bookings.remove(bookingId);
        saveBookingsToFile();
        System.out.printf("Booking %s cancelled. Refund of ₹%.2f simulated.%n", bookingId, b.getAmount());
    }

    private void viewMyBookings() {
        String userName = prompt("Enter your name to view your bookings");
        boolean found = false;
        for (Booking b : bookings.values()) {
            if (b.getUserName().equalsIgnoreCase(userName)) {
                System.out.println(b);
                found = true;
            }
        }
        if (!found) System.out.println("No bookings found for user: " + userName);
    }

    /* ---------- Helper methods ---------- */

    private List<String> parseSeatList(String input) {
        String[] parts = input.split(",");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            p = p.trim().toUpperCase();
            if (!p.isEmpty()) out.add(p);
        }
        return out;
    }

    private boolean simulatePayment(double amount) {
        System.out.println("=== Payment Gateway (simulated) ===");
        System.out.println("Amount to pay: ₹" + String.format("%.2f", amount));
        System.out.println("Choose payment method:");
        System.out.println("1. Card");
        System.out.println("2. NetBanking");
        System.out.println("3. UPI");
        String choice = prompt("Enter payment option (1-3)");
        if (!Arrays.asList("1","2","3").contains(choice)) {
            System.out.println("Invalid payment option.");
            return false;
        }
        String dummy = prompt("Enter any dummy payment credential to proceed (simulated)");
        System.out.println("Processing payment...");
        // simulate slight delay
        try { Thread.sleep(700); } catch (InterruptedException ignored) {}
        System.out.println("Payment successful.");
        return true;
    }

    private String generateBookingId() {
        return "B-" + String.format("%04d", bookings.size() + 1 + new Random().nextInt(999));
    }

    private String prompt(String message) {
        System.out.print(message + ": ");
        return scanner.nextLine().trim();
    }

    /* ---------- Main ---------- */

    public static void main(String[] args) {
        MovieBookingSystem app = new MovieBookingSystem();
        app.start();
    }
}
