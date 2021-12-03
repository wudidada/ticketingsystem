package ticketingsystem;

import java.util.*;

public class TicketingDS implements TicketingSystem {

    long current_tid = 0;
    
    List<List<Set<Seat>>> empty_seat_list;
    
    Map<Long, Ticket> tid_ticket_map;
    
    Map<Long, Seat> tid_seat_map;
    
    public TicketingDS(int routenum, int coachnum, int seatnum, int stationnum, int threadnum) {
        List<List<Set<Seat>>> empty_seat_route = new ArrayList<>();
        empty_seat_route.add(null);
        for (int route = 1; i <= routenum; i++) {
            List<Set<Seat>> empty_seat_station = new ArrayList<>();
            empty_seat_station.add(null);
            for (int station = 1; station <= stationnum; station++) {
                Set<Seat> empty_seat = new HashSet<>();
                for (int coach = 1; coach <= coachnum; coach++) {
                    for (int seat = 1; seat <= seatnum; seat++) {
                        empty_seat.add(new Seat(coach, seat));
                    }
                }
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
        for (int i = departure + 1; !empty_seat.isEmpty() && i <= arrival; i++) {
            empty_seat.retainAll(empty_seat_list.get(i));
        }
        return empty_seat;
    }

    public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
        Set<Seat> empty_seat = get_empty_seat(route, departure, arrival);
        if (empty_seat.size() == 0)
            return null;

        List<Set<Seat>> empty_seat_list = getRoute(route);


        return null;
    }

    // 返回余票数
    public int inquiry(int route, int departure, int arrival) {
        return 0;
    }

    public boolean refundTicketReplay(Ticket ticket) {
        return false;
    }

    public boolean buyTicketReplay(Ticket ticket) {
        return false;
    }

    public boolean refundTicket(Ticket ticket) {
        return false;
    }

    public static void main(String[] args) {
        Seat a = new Seat(1,1);
        Seat b = new Seat(1, 1);

        Map<Seat, Integer> map = new HashMap<>();
        map.put(a, 1);
        map.put(b, 2);

    }


}

class Seat {
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
}

