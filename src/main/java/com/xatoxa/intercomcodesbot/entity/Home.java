package com.xatoxa.intercomcodesbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.telegram.telegrambots.meta.api.objects.Location;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity(name="homes")
public class Home extends HomeAbstract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Double lon;

    @Column
    private Double lat;

    @Column
    private String street;

    @Column
    private String number;

    @OneToMany(mappedBy = "home", cascade = CascadeType.REMOVE, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<Entrance> entrances;

    public Location getLocation(){
        Location location = new Location();
        location.setLongitude(this.lon);
        location.setLatitude(this.lat);

        return location;
    }

    public void addEntrance(Entrance entrance){
        if (this.entrances == null){
            this.entrances = new ArrayList<>();
        }
        this.entrances.add(entrance);
    }

    public void delEntrance(Entrance entrance){
        this.entrances.remove(entrance);
    }

    public void fillAddressFromMsg(String msgText) {
        String[] address = msgText.split(", ");
        this.street = address[0];
        this.number = address[1];
    }

    public void fillCoordsFromLocation(Location location) {
        this.lon = location.getLongitude();
        this.lat = location.getLatitude();
    }

    public String getAllTextCodes(){
        StringBuilder text = new StringBuilder();
        text
                .append(this.getAddress())
                .append("\n");

        this.entrances.sort(Comparator.comparing(Entrance::getNumber));

        for (Entrance entrance:
             this.entrances) {
            text.append("\n");
            text.append("Подъезд ");
            text.append(entrance.getNumber());
            text.append(entrance.getTextCodes());
        }

        return text.toString();
    }

    @Override
    public String getAddress() {
        return this.street +
                ", " +
                this.number;
    }
}
