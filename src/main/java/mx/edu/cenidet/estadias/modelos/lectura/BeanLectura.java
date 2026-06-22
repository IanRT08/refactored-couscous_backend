package mx.edu.cenidet.estadias.modelos.lectura;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Lectura", uniqueConstraints = @UniqueConstraint(columnNames = "fechaLectura"))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(of = "idLectura")

public class BeanLectura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idLectura;

    @Column(name = "fechaLectura", nullable = false, unique = true)
    private LocalDateTime fechaLectura;

    @Column(name = "temperatura")
    private Float temperatura;

    @Column(name = "viento")
    private Float viento;

    @Column(name = "humedad")
    private Float humedad;

    @Column(name = "radiacion")
    private Float radiacion;

    @Column(name = "presion")
    private Float presion;

    @Column(name = "rainDate")
    private Float rainRate;

    @Column(name = "dailyRain")
    private Float dailyRain;

    @Column(name = "rainTotal")
    private Float rainTotal;

}
