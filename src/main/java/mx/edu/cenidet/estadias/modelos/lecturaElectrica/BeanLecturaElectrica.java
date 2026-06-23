package mx.edu.cenidet.estadias.modelos.lecturaElectrica;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "LecturaElectrica", uniqueConstraints = @UniqueConstraint(columnNames = {"fechaLectura", "fuente"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "idLecturaElectrica")

public class BeanLecturaElectrica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idLecturaElectrica;

    @Column(name = "fechaLectura", nullable = false)
    private LocalDateTime fechaLectura;

    //Discriminador: de cuál de los dos canales de ThingSpeak viene la lectura
    @Enumerated(EnumType.STRING)
    @Column(name = "fuente", length = 20, nullable = false)
    private FuenteElectrica fuente;

    //Fotovoltaico: Vload | Eólico: Voltaje
    @Column(name = "voltaje")
    private Float voltaje;

    //Fotovoltaico: iload | Eólico: Corriente
    @Column(name = "corriente")
    private Float corriente;

    //Fotovoltaico: Power | Eólico: Potencia
    @Column(name = "potencia")
    private Float potencia;

    //Exclusivo de Fotovoltaico (voltaje de circuito abierto). Null para Eólico.
    @Column(name = "voc")
    private Float voc;

    //No la reporta ThingSpeak: se calcula integrando potencia x tiempo desde
    //la lectura anterior de la misma fuente (energía incremental, en Wh).
    @Column(name = "energia")
    private Float energia;

}
