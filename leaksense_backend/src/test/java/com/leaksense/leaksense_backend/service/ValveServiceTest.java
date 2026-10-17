package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.entity.ValveAction;
import com.leaksense.leaksense_backend.entity.Zone;
import com.leaksense.leaksense_backend.repository.AlertRepository;
import com.leaksense.leaksense_backend.repository.SensorNodeRepository;
import com.leaksense.leaksense_backend.repository.ValveActionRepository;
import com.leaksense.leaksense_backend.repository.ZoneRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValveServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private SensorNodeRepository nodeRepository;

    @Mock
    private ValveActionRepository valveActionRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private CommandService commandService;

    @InjectMocks
    private ValveService valveService;

    private final Zone zone = new Zone(1L, "Zone A", "Main pipeline", "SAFE");

    private String stateOf(String... valves) {
        List<SensorNode> nodes = new ArrayList<>();
        long id = 1;
        for (String valve : valves) {
            nodes.add(new SensorNode(id, "NODE-" + id, "ONLINE", 80, LocalDateTime.now(), zone, valve));
            id++;
        }
        when(zoneRepository.findAll()).thenReturn(List.of(zone));
        when(nodeRepository.findByZoneId(1L)).thenReturn(nodes);
        return valveService.getZoneStatuses().get(0).getValveState();
    }

    @Test
    @DisplayName("Nodes that all agree give that state")
    void allNodesAgree() {
        assertThat(stateOf("CLOSED", "CLOSED")).isEqualTo("CLOSED");
        assertThat(stateOf("OPEN", "OPEN")).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("A node without a saved state counts as OPEN")
    void missingStateCountsAsOpen() {
        assertThat(stateOf((String) null, "OPEN")).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("Open and closed nodes together give PARTIAL")
    void mixedStatesArePartial() {
        assertThat(stateOf("OPEN", "CLOSED")).isEqualTo("PARTIAL");
    }

    @Test
    @DisplayName("A valve that is moving beats a settled one")
    void movingStateWins() {
        assertThat(stateOf("CLOSED", "CLOSING")).isEqualTo("CLOSING");
        assertThat(stateOf("OPEN", "OPENING")).isEqualTo("OPENING");
    }

    @Test
    @DisplayName("A faulty valve beats every other state")
    void faultWins() {
        assertThat(stateOf("CLOSING", "FAULT", "OPEN")).isEqualTo("FAULT");
    }

    @Test
    @DisplayName("A zone without nodes uses its last valve action")
    void zoneWithoutNodesUsesLastAction() {
        when(zoneRepository.findAll()).thenReturn(List.of(zone));
        when(nodeRepository.findByZoneId(1L)).thenReturn(List.of());
        when(valveActionRepository.findFirstByZoneIdOrderByPerformedAtDesc(1L)).thenReturn(
                Optional.of(new ValveAction(1L, "CLOSE", "SYSTEM", "reason", "SYSTEM",
                        LocalDateTime.now(), zone)));

        assertThat(valveService.getZoneStatuses().get(0).getValveState()).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("A zone without nodes or actions counts as OPEN")
    void zoneWithoutAnythingIsOpen() {
        when(zoneRepository.findAll()).thenReturn(List.of(zone));
        when(nodeRepository.findByZoneId(1L)).thenReturn(List.of());
        when(valveActionRepository.findFirstByZoneIdOrderByPerformedAtDesc(1L))
                .thenReturn(Optional.empty());

        assertThat(valveService.getZoneStatuses().get(0).getValveState()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("An unknown zone is rejected")
    void unknownZone() {
        when(zoneRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> valveService.setValve(9L, "CLOSE", "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Zone not found: 9");
    }

    @Test
    @DisplayName("An action other than OPEN or CLOSE is rejected")
    void invalidAction() {
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));

        assertThatThrownBy(() -> valveService.setValve(1L, "EXPLODE", "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Action must be OPEN or CLOSE");
        verifyNoInteractions(commandService);
    }

    @Test
    @DisplayName("Opening is blocked while a CRITICAL leak alert is open")
    void openBlockedDuringCriticalLeak() {
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(alertRepository.existsByZoneIdAndLevelAndSeverityAndStatusNot(
                1L, "SENSOR", "CRITICAL", "RESOLVED")).thenReturn(true);

        assertThatThrownBy(() -> valveService.setValve(1L, "OPEN", "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot open the valve while a CRITICAL leak alert is open");
        verifyNoInteractions(commandService);
    }

    @Test
    @DisplayName("Closing is allowed even during a CRITICAL leak, in any letter case")
    void closeAlwaysAllowed() {
        ValveAction action = new ValveAction(1L, "CLOSE", "admin", "Manual close from dashboard",
                "dashboard", LocalDateTime.now(), zone);
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(commandService.issueZoneCommand(zone, "CLOSE_VALVE", "MANUAL", "admin",
                "Manual close from dashboard")).thenReturn(action);

        assertThat(valveService.setValve(1L, "close", "admin")).isSameAs(action);
    }

    @Test
    @DisplayName("Opening goes ahead when no CRITICAL leak is open")
    void openAllowedWhenSafe() {
        ValveAction action = new ValveAction(2L, "OPEN", "admin", "Manual open from dashboard",
                "dashboard", LocalDateTime.now(), zone);
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(commandService.issueZoneCommand(zone, "OPEN_VALVE", "MANUAL", "admin",
                "Manual open from dashboard")).thenReturn(action);

        assertThat(valveService.setValve(1L, "OPEN", "admin")).isSameAs(action);
    }

    @Test
    @DisplayName("A valve that is already in the wanted state gives a clear message")
    void alreadyInWantedState() {
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(commandService.issueZoneCommand(zone, "CLOSE_VALVE", "MANUAL", "admin",
                "Manual close from dashboard")).thenReturn(null);

        assertThatThrownBy(() -> valveService.setValve(1L, "CLOSE", "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The valve in Zone A is already closed or closing");
    }

    @Test
    @DisplayName("Opening is denied if user is not an administrator")
    void openingDeniedForNonAdmin() {
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));

        assertThatThrownBy(() -> valveService.setValve(1L, "OPEN", "operator", false))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Only administrators are permitted to open pipeline valves.");
        verifyNoInteractions(commandService);
    }

    @Test
    @DisplayName("Emergency closing is permitted for operators")
    void emergencyClosingAllowedForOperators() {
        ValveAction action = new ValveAction(1L, "CLOSE", "operator", "Manual close from dashboard",
                "dashboard", LocalDateTime.now(), zone);
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(commandService.issueZoneCommand(zone, "CLOSE_VALVE", "MANUAL", "operator",
                "Manual close from dashboard")).thenReturn(action);

        assertThat(valveService.setValve(1L, "CLOSE", "operator", false)).isSameAs(action);
    }
}
