package ticketingsystem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TicketingDS implements TicketingSystem {

    AtomicLong current_tid = new AtomicLong(0);

    List<List<Set<Seat>>> empty_seat_list;

    Map<Long, Ticket> tid_ticket_map = new ConcurrentHashMap<>();

    Map<Long, Seat> tid_seat_map = new ConcurrentHashMap<>();

    public TicketingDS(int routenum, int coachnum, int seatnum, int stationnum, int threadnum) {
        List<List<Set<Seat>>> empty_seat_route = new ArrayList<>();
        empty_seat_route.add(null);

        List<Seat> seats = new ArrayList<>();
        for (int coach = 1; coach <= coachnum; coach++) {
            for (int seat = 1; seat <= seatnum; seat++) {
                seats.add(new Seat(coach, seat));
            }
        }

        for (int route = 1; route <= routenum; route++) {
            List<Set<Seat>> empty_seat_station = new ArrayList<>();
            empty_seat_station.add(null);
            for (int station = 1; station <= stationnum; station++) {
                Set<Seat> empty_seat = ConcurrentHashMap.newKeySet();
                empty_seat.addAll(seats);
                empty_seat_station.add(empty_seat);
            }
            empty_seat_route.add(empty_seat_station);
        }
        this.empty_seat_list = empty_seat_route;
    }

    private List<Set<Seat>> getRoute(int route) {
        return this.empty_seat_list.get(route);
    }

    private Set<Seat> getStation(int route, int station) {
        return this.empty_seat_list.get(route).get(station);
    }

    private Set<Seat> get_empty_seat(int route, int departure, int arrival) {
        List<Set<Seat>> empty_seat_list = getRoute(route);
        Set<Seat> empty_seat = new HashSet<>(empty_seat_list.get(departure));
        for (int i = departure + 1; !empty_seat.isEmpty() && i < arrival; i++) {
            empty_seat.retainAll(empty_seat_list.get(i));
        }
        return empty_seat;
    }

    public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
        Set<Seat> empty_seat = get_empty_seat(route, departure, arrival);
        if (empty_seat.size() == 0)
            return null;

        Seat my_seat = get_first_seat(empty_seat);

        List<Set<Seat>> station_empty_seats = getRoute(route);
        int station = departure;
        boolean success = true;

        while (station < arrival && success) {
            success = station_empty_seats.get(station).remove(my_seat);
            station += 1;
        }

        if (!success) {
            for(int i = departure; i < station; i++) {
                station_empty_seats.get(i).add(my_seat);
            }
            return buyTicket(passenger, route, departure, arrival);
        }

        long tid = current_tid.addAndGet(1);
        Ticket ticket = new Ticket();
        ticket.tid = tid;
        ticket.coach = my_seat.coach;
        ticket.seat = my_seat.seat;
        ticket.arrival = arrival;
        ticket.departure = departure;
        ticket.passenger = passenger;
        ticket.route = route;

        tid_ticket_map.put(tid, ticket);
        tid_seat_map.put(tid, my_seat);

        return ticket;
    }

    private Seat get_first_seat(Set<Seat> empty_seat) {
        return new TreeSet<>(empty_seat).first();
    }

    // 返回余票数
    public int inquiry(int route, int departure, int arrival) {
        return get_empty_seat(route, departure, arrival).size();
    }

    @Override
    public boolean refundTicketReplay(Ticket ticket) {
        return false;
    }

    @Override
    public boolean buyTicketReplay(Ticket ticket) {
        return false;
    }

    public boolean refundTicket(Ticket ticket) {
        Ticket sys_ticket = tid_ticket_map.get(ticket.tid);

        if (sys_ticket == null) {
            return false;
        }

        if (sys_ticket.route != ticket.route || sys_ticket.coach != ticket.coach || sys_ticket.arrival != ticket.arrival || sys_ticket.seat != ticket.seat || sys_ticket.departure != ticket.departure || !sys_ticket.passenger.equals(ticket.passenger)) {
            return false;
        }

        Ticket removed_ticket = tid_ticket_map.remove(sys_ticket.tid);
        if (removed_ticket == null) {
            return false;
        }

        Seat seat = tid_seat_map.remove(sys_ticket.tid);

        List<Set<Seat>> station_seat = getRoute(sys_ticket.route);
        for (int station = sys_ticket.departure; station < sys_ticket.arrival; station++) {
            station_seat.get(station).add(seat);
        }

        return true;
    }

    public static void main(String[] args) {
        Set<Seat> s = new HashSet<>();
        s.add(new Seat(1,1));
        s.add(new Seat(1,2));
        s.add(new Seat(1,3));
        s.add(new Seat(2,1));
        s.add(new Seat(2,2));
        s.add(new Seat(2,3));
        s.add(new Seat(3,1));
        s.add(new Seat(3,2));
        s.add(new Seat(3,3));

        Set<Seat> t = new TreeSet<>(s);
        System.out.printf("" + t.size());

    }
}

class Seat implements Comparable<Seat> {
    int coach;
    int seat;

    public Seat(int coach, int seat) {
        this.coach = coach;
        this.seat = seat;
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Seat seat1 = (Seat) o;
        return coach == seat1.coach && seat == seat1.seat;
    }

    @Override public int hashCode() {
        return Objects.hash(coach, seat);
    }

    @Override
    public int compareTo(Seat s) {
        int t = coach - s.coach;
        if (t != 0) {
            return t;
        }
        return seat - s.seat;
    }
}

