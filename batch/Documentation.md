Architecture Overview  OBSOLETE                                                                                                                                                           
                                                                                                                                                                                    
  The batch module implements a classic Spring Batch pipeline: Reader → Processor → Writer, triggered by a scheduler, with a completion listener that updates a downstream cache.   
                                                                                                                                                                                    
  ---                                                                                                                                                                               
  Data Flow                                                                                                                                                                         

  TleJobScheduler
        │
        ▼
    tleFetchJob (BatchConfig)
        │
        ▼
    fetchTleStep
        │
    ┌───┴────────────────────┐
    │                        │
  TleItemReader          (on complete)
    │    ▼                   │
    │  Space-Track   TleJobCompletionListener
    │  CelesTrak             │
    │                        ▼
    ▼              PropagatorCacheService
  TleItemProcessor       .rebuildCache()
    │
    ▼
  TleItemWriter
    │
    ▼
  TleRepository.upsert()
    │
    ▼
  PostgreSQL (tles table)

  ---
  Classes Explained

  Configuration

  BatchConfig
  Declares the Spring Batch Job and Step beans. The step is chunk-oriented: it reads, processes, and writes chunkSize records (default 500) per database transaction.

  TleJobProperties
  Maps sattrack.batch.tle.* from application.yml to a typed object. Holds group (object type filter), chunkSize, and cron.

  SpaceTrackProperties
  Maps spacetrack.identity and spacetrack.password from config (populated via env vars SPACETRACK_IDENTITY / SPACETRACK_PASSWORD).

  RestClientConfig
  Creates two RestClient beans — one for Space-Track (with a CookieManager for session auth, 5-min read timeout) and one for CelesTrak (public API, no cookie handling needed).

  ---
  Batch Pipeline

  TleItemReader (ItemReader<TleRecord>)
  Called once per record. On the first call it bulk-fetches from Space-Track; if that fails it falls back to CelesTrak. Holds the list internally and returns one TleRecord at a
  time until exhausted (returns null to signal end-of-input to Spring Batch).

  TleRecord (Java record)
  An immutable DTO carrying noradCatId, name, line1, line2, epoch, and source between the Reader and Processor. It's never persisted directly.

  TleItemProcessor (ItemProcessor<TleRecord, Tle>)
  Validates each TleRecord (non-null fields, NORAD ID > 0, line format starting with "1 " / "2 ", minimum 69 chars). Returns null for invalid records (Spring Batch skips them).
  Converts valid records to Tle JPA entities.

  TleItemWriter (ItemWriter<Tle>)
  Receives a Chunk<Tle> and calls TleRepository.upsert() for each entity. The whole chunk runs in a single transaction managed by Spring Batch.

  ---
  Scheduling & Listening

  TleJobScheduler
  Uses @Scheduled to fire periodically (currently every 2 minutes for testing; production default is 0 0 2 * * * = 2 AM UTC). Generates a unique run.id parameter per launch so
  Spring Batch doesn't consider it a re-run of a previous job instance.

  TleJobCompletionListener (JobExecutionListener)
  Runs after the job finishes. Logs read/write counts and duration. If the exit status is COMPLETED, it calls PropagatorCacheService.rebuildCache() to refresh the in-memory
  propagator cache with the freshly stored TLEs.

  ---
  External Data Sources

  SpaceTrackBulkService
  Authenticates to space-track.org with a form POST, then fetches all TLEs in one request (3LE format). Parses the response into TleRecord objects, extracting NORAD ID from columns
   3–7 of line 1 and epoch from columns 19–32.

  CelesTrakBulkService
  Hits the public CelesTrak API (/NORAD/elements/gp.php). Maps Space-Track object types (PAYLOAD, DEBRIS, ROCKET BODY) to CelesTrak group names. No authentication needed.

  ---
  Common Module

  Tle (JPA Entity, table tles)
  The persisted representation. Key fields: noradCatId (unique), line1, line2, epoch, satelliteName, source, fetchedAt.

  TleRepository
  Standard JpaRepository plus a native PostgreSQL INSERT ... ON CONFLICT (norad_cat_id) DO UPDATE query (upsert()). This is atomic — no separate select-then-insert logic needed.

  PropagatorCacheService (interface)
  Single method: rebuildCache(). Decouples the batch module from the implementation detail of how the cache is rebuilt. SatellitePositionService in the API module implements this.

  SatellitePositionService
  Implements rebuildCache(): clears the ConcurrentHashMap, loads all Tle rows from the database, instantiates an Orekit TLEPropagator (SGP4 algorithm) for each, and stores them
  keyed by NORAD ID. Subsequent position requests use these cached propagators instead of re-parsing TLEs every time.

