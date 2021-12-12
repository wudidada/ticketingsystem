package ticketingsystem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TicketingDS implements TicketingSystem {

    AtomicLong current_tid = new AtomicLong(0);

    List<List<BitSet>> empty_seat_list;

    List<List<Lock>> lock_list;

    Map<Long, Ticket> tid_ticket_map = new ConcurrentHashMap<>();

    int total_seat_num;

    int seat_num;

    int coach_num;


    public TicketingDS(int routenum, int coachnum, int seatnum, int stationnum, int threadnum) {
        List<List<BitSet>> empty_seat_route = new ArrayList<>();
        List<List<Lock>> lock_route = new ArrayList<>();
        empty_seat_route.add(null);
        lock_route.add(null);

        total_seat_num = coachnum * seatnum;
        coach_num = coachnum;
        seat_num = seatnum;

        for (int route = 1; route <= routenum; route++) {
            List<BitSet> empty_seat_station = new ArrayList<>();
            List<Lock> lock_station = new ArrayList<>();
            empty_seat_station.add(null);
            lock_station.add(null);
            for (int station = 1; station <= stationnum; station++) {
                BitSet empty_seat = new BitSet(total_seat_num);
                empty_seat.set(0, total_seat_num, true);
                empty_seat_station.add(empty_seat);
                lock_station.add(new ReentrantLock());
            }
            empty_seat_route.add(empty_seat_station);
            lock_route.add(lock_station);
        }
        this.empty_seat_list = empty_seat_route;
        this.lock_list = lock_route;
    }

    private List<BitSet> getRoute(int route) {
        return empty_seat_list.get(route);
    }

    private BitSet get_empty_seat(int route, int departure, int arrival) {
        List<BitSet> empty_seat_list = getRoute(route);
        BitSet empty_seat = (BitSet) empty_seat_list.get(departure).clone();

        for (int i = departure + 1;  empty_seat.cardinality() >0 && i < arrival; i++) {
            empty_seat.and(empty_seat_list.get(i));
        }
        return empty_seat;
    }

    public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
        BitSet empty_seat = get_empty_seat(route, departure, arrival);
        if (empty_seat.cardinality() == 0)
            return null;

        int seat_index = get_first_seat(empty_seat);

        List<BitSet> station_empty_seats = getRoute(route);
        List<Lock> route_lock = lock_list.get(route);

        int station = departure;
        boolean success = true;

        while (station < arrival) {
            route_lock.get(station).lock();
            if (!station_empty_seats.get(station).get(seat_index)) {
                route_lock.get(station).unlock();
                success = false;
                break;
            }
            station_empty_seats.get(station).set(seat_index, false);
            route_lock.get(station).unlock();
            station += 1;
        }

        if (!success) {
            for(int i = departure; i < station; i++) {
                route_lock.get(i).lock();
                station_empty_seats.get(i).set(seat_index, true);
                route_lock.get(i).unlock();
            }
            return buyTicket(passenger, route, departure, arrival);
        }

        long tid = current_tid.addAndGet(1);
        Ticket ticket = new Ticket();
        ticket.tid = tid;
        ticket.coach = index_to_coach(seat_index);
        ticket.seat = index_to_seat(seat_index);
        ticket.arrival = arrival;
        ticket.departure = departure;
        ticket.passenger = passenger;
        ticket.route = route;

        tid_ticket_map.put(tid, ticket);

        return ticket;
    }

    private int index_to_coach(int seat_index) {
        return seat_index / seat_num + 1;
    }

    private int index_to_seat(int seat_index) {
        return seat_index % seat_num + 1;
    }


    private int get_first_seat(BitSet empty_seats) {
        for (int i = 0; i < total_seat_num; i++) {
            if (empty_seats.get(i)) {
                return i;
            }
        }

        System.err.println("get empty seat failed.");

        return -1;
    }

    // 返回余票数
    public int inquiry(int route, int departure, int arrival) {
        return get_empty_seat(route, departure, arrival).cardinality();
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

        int seat_index = get_seat_index(sys_ticket.coach, sys_ticket.seat);

        List<BitSet> station_seat = getRoute(sys_ticket.route);
        List<Lock> station_lock = lock_list.get(sys_ticket.route);
        for (int station = sys_ticket.departure; station < sys_ticket.arrival; station++) {
            station_lock.get(station).lock();
            station_seat.get(station).set(seat_index, true);
            station_lock.get(station).unlock();
        }

        return true;
    }

    private int get_seat_index(int coach, int seat) {
        return (coach - 1) * seat_num + seat - 1;
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

