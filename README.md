# OWMETA.GG - Overwatch Statistics Website (Backend)

This repository contains the backend service for owmeta.gg, a website dedicated to collecting, analyzing, and displaying Overwatch 2 player statistics. 

## Table of Contents
- [Changelog](#changelog)
  - [August 2025](#august-2025)

## Changelog

### August 2025
**08/30/2025**: Enhanced database performance and prepared for scaling
- Added 26 performance indexes across all tables for 100k+ player optimization
- Implemented HikariCP connection pooling with production-ready configuration
- Created comprehensive performance testing suite with EXPLAIN ANALYZE queries
- Fixed Flyway migration issues with concurrent index creation
- Added database performance monitoring and troubleshooting tools
- Verified index usage and query optimization for sub-millisecond response times

**08/27/2025**: Implemented complete statistics pipeline with event-driven architecture
- Added `StatisticsCalculationService` for automatic stats calculation on player data updates
- Created statistics tables: `hero_statistics`, `rank_distribution`, `hero_trends`, `role_statistics`
- Implemented REST API endpoints for statistics retrieval with caching
- Fixed NULL handling issues in SQL queries with COALESCE
- Corrected pick rate calculations to use total player base
- Added HikariCP connection pooling configuration optimized for 100k+ players
- Created comprehensive testing guide and SQL debug scripts

**08/26/2025**: Major refactoring from entity-based to model-based architecture
- Migrated from `entities` package to `models` package structure
- Implemented `PlayerProcessingService` with RabbitMQ integration
- Added OverFast API integration for player data fetching
- Created event system with `PlayerDataProcessedEvent`
- Set up Flyway migrations for database versioning
- Implemented hero stats tracking per player per game mode

**08/25/2025**: Massive project overhaul setup and configuration
- Set up Spring Boot application with PostgreSQL and RabbitMQ
- Created Docker Compose configuration for local development
- Implemented player and hero data models
- Added RabbitMQ configuration for asynchronous processing
- Set up development and production Spring profiles
