package com.zaig100.dargon.ecs;

/** Компоненты для тестов. */
final class TestComponents {

    private TestComponents() {}

    static final class Position implements Component {
        int x;
        int y;

        Position set(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        @Override
        public void reset() {
            x = 0;
            y = 0;
        }
    }

    static final class Velocity implements Component {
        int dx;
        int dy;

        @Override
        public void reset() {
            dx = 0;
            dy = 0;
        }
    }

    static final class Health implements Component {
        int hp;

        @Override
        public void reset() {
            hp = 0;
        }
    }

    /** Маркер без данных. */
    static final class Frozen implements Component {
        @Override
        public void reset() {}
    }

    static WorldBuilder builder() {
        return new WorldBuilder()
                .component(Position.class, Position::new)
                .component(Velocity.class, Velocity::new)
                .component(Health.class, Health::new)
                .component(Frozen.class, Frozen::new);
    }
}
