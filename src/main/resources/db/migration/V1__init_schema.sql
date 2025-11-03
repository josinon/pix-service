create table wallet (
  id uuid primary key,
  status varchar(16) not null default 'ACTIVE',
  created_at timestamptz not null default now(),
  version int not null default 0
);

create table pix_key (
  id uuid primary key,
  wallet_id uuid not null references wallet(id),
  type varchar(16) not null,
  value varchar(255) not null,
  status varchar(16) not null default 'ACTIVE',
  created_at timestamptz not null default now(),
  revoked_at timestamptz null,
  constraint uq_pix_active unique (type, value, status),
  CONSTRAINT fk_pix_key_wallet FOREIGN KEY (wallet_id) REFERENCES wallet(id)
);