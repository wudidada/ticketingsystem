package ticketingsystem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TicketingDS implements TicketingSystem {

    AtomicLong current_tid = new AtomicLong(0);
    
    List<List<Set<Seat>>> empty_seat_list;

    List<List<ReentrantReadWriteLock>> lock_list;
    
    Map<Long, Ticket> tid_ticket_map = new HashMap<>();
    
    Map<Long, Seat> tid_seat_map = new ConcurrentHashMap<>();

    final ReentrantReadWriteLock ticket_lock = new ReentrantReadWriteLock();

    public TicketingDS(int routenum, int coachnum, int seatnum, int stationnum, int threadnum) {
        List<List<Set<Seat>>> empty_seat_route = new ArrayList<>();
        List<List<ReentrantReadWriteLock>> lock_route = new ArrayList<>();
        empty_seat_route.add(null);
        lock_route.add(null);
        for (int route = 1; route <= routenum; route++) {
            List<Set<Seat>> empty_seat_station = new ArrayList<>();
            List<ReentrantReadWriteLock> lock_station = new ArrayList<>();
            empty_seat_station.add(null);
            lock_station.add(null);
            for (int station = 1; station <= stationnum; station++) {
                Set<Seat> empty_seat = new HashSet<>();
                for (int coach = 1; coach <= coachnum; coach++) {
                    for (int seat = 1; seat <= seatnum; seat++) {
                        empty_seat.add(new Seat(coach, seat));
                    }
                }
                empty_seat_station.add(empty_seat);
                lock_station.add(new ReentrantReadWriteLock());
            }
            empty_seat_route.add(empty_seat_station);
            lock_route.add(lock_station);
        }
        this.empty_seat_list = empty_seat_route;
        this.lock_list = lock_route;
    }

    private List<Set<Seat>> getRoute(int route) {
        return this.empty_seat_list.get(route);
    }

    private Set<Seat> getStation(int route, int station) {
        return this.empty_seat_list.get(route).get(station);
    }

    private Set<Seat> get_empty_seat(int route, int departure, int arrival) {
        List<Set<Seat>> empty_seat_list = getRoute(route);
        List<ReentrantReadWriteLock> route_lock = lock_list.get(route);

        route_lock.get(departure).readLock().lock();
        Set<Seat> empty_seat = new TreeSet<>(empty_seat_list.get(departure));
        route_lock.get(departure).readLock().unlock();

        for (int i = departure + 1; !empty_seat.isEmpty() && i < arrival; i++) {
            Lock rl = route_lock.get(i).readLock();

            rl.lock();
            empty_seat.retainAll(empty_seat_list.get(i));
            rl.unlock();
        }
        return empty_seat;
    }

    public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
        Set<Seat> empty_seat = get_empty_seat(route, departure, arrival);
        if (empty_seat.size() == 0)
            return null;

        Seat my_seat = ((TreeSet<Seat>) empty_seat).first();

        List<Set<Seat>> station_empty_seats = getRoute(route);
        List<ReentrantReadWriteLock> route_lock = lock_list.get(route);

        int station = departure;
        boolean success = true;

        while (station < arrival && success) {
            Lock wl = route_lock.get(station).writeLock();

            wl.lock();
            success = station_empty_seats.get(station).remove(my_seat);
            wl.unlock();

            station += 1;
        }

        if (!success) {
            for(int i = departure; i < station; i++) {
                Lock wl = route_lock.get(i).writeLock();

                wl.lock();
                station_empty_seats.get(i).add(my_seat);
                wl.unlock();
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

        ticket_lock.writeLock().lock();
        tid_ticket_map.put(tid, ticket);
        ticket_lock.writeLock().unlock();

        tid_seat_map.put(tid, my_seat);

        return ticket;
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
        ticket_lock.readLock().lock();
        Ticket sys_ticket = tid_ticket_map.get(ticket.tid);
        ticket_lock.readLock().unlock();

        if (sys_ticket == null) {
            return false;
        }

        if (sys_ticket.route != ticket.route || sys_ticket.coach != ticket.coach || sys_ticket.arrival != ticket.arrival || sys_ticket.seat != ticket.seat || sys_ticket.departure != ticket.departure || !sys_ticket.passenger.equals(ticket.passenger)) {
            return false;
        }

        ticket_lock.writeLock().lock();
        if (tid_ticket_map.get(sys_ticket.tid) == null) {
            ticket_lock.writeLock().unlock();
            return false;
        }

        tid_ticket_map.remove(sys_ticket.tid);
        ticket_lock.writeLock().unlock();

        Seat seat = tid_seat_map.remove(sys_ticket.tid);

        List<Set<Seat>> station_seat = getRoute(sys_ticket.route);
        List<ReentrantReadWriteLock> route_lock = lock_list.get(sys_ticket.route);
        for (int station = sys_ticket.departure; station < sys_ticket.arrival; station++) {
            Lock rl = route_lock.get(station).writeLock();

            rl.lock();
            station_seat.get(station).add(seat);
            rl.unlock();
        }

        return true;
    }

    public static void main(String[] args) {
        Set<Seat> s = new HashSet<>();
        System.out.println(s.hashCode());

        s.add(new Seat(1, 2));
        System.out.println(s.hashCode());

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

