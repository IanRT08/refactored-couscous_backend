package mx.edu.cenidet.estadias.modelos.lecturaElectrica;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "LecturaElectrica", uniqueConstraints = @UniqueConstraint(columnNames = "fechaLectura"))
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

    @Column(name = "fechaLectura", nullable = false, unique = true)
    private LocalDateTime fechaLectura;

    @Column(name = "corriente")
    private Float corriente;

    @Column(name = "voltaje")
    private Float voltaje;

    @Column(name = "potencia")
    private Float potencia;

    @Column(name = "energia")
    private Float energia;

}
