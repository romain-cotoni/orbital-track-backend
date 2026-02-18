package space.satellite.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import space.satellite.dtos.SatelliteRequestDto;
import space.satellite.dtos.SatelliteResponseDto;
import space.satellite.services.SatellitePositionService;

import java.util.List;

import static space.satellite.constants.Constants.SPACETRACK_SOURCE;

@RestController
@RequestMapping(value = {"/position"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SatellitePositionController {

    private final SatellitePositionService satellitePositionService;

    @GetMapping
    public ResponseEntity<List<SatelliteResponseDto>> getListOfSatellitesToTrack() {
        List<SatelliteResponseDto> satellites = null;
        return ResponseEntity.ok(satellites);
    }

    @PostMapping
    public ResponseEntity<List<SatelliteResponseDto>> getPositions(@RequestBody List<SatelliteRequestDto> request) {
        List<SatelliteResponseDto> positions = satellitePositionService.getPositions(request, SPACETRACK_SOURCE);
        return ResponseEntity.ok(positions);
    }


    @PostMapping("/single")
    public ResponseEntity<SatelliteResponseDto> getPosition(@RequestBody SatelliteRequestDto dto) {
        SatelliteResponseDto responseDto = satellitePositionService.getPosition(dto);
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<SatelliteResponseDto>> getBatchPositions(@RequestBody List<SatelliteRequestDto> catalogNumbers) {
        List<SatelliteResponseDto> positions = catalogNumbers.stream()
                .map(satellitePositionService::getPosition)
                .toList();
        return ResponseEntity.ok(positions);
    }
}
