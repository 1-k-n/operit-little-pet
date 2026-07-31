-- Operit 小精灵后端表结构
-- 在 Supabase SQL Editor 里执行此文件即可

-- 1. 状态推送表：用户/AI 写入状态(表情/气泡/反应)，悬浮窗轮询读取显示
create table if not exists public.pet_state (
    id bigint generated always as identity primary key,
    payload jsonb not null default '{}'::jsonb,   -- {emotion, bubble, ts, ...}
    created_at timestamptz not null default now()
);

-- 顺手建个索引方便按时间倒序取最新
create index if not exists pet_state_created_idx
    on public.pet_state (created_at desc);

-- 2. 感知状态记录表（可选）：记录小精灵读到的环境状态
create table if not exists public.pet_perception (
    id bigint generated always as identity primary key,
    payload jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);

-- 3. 给匿名角色开读/写权限（开发期最简单；正式可收紧为 authenticated）
alter table public.pet_state enable row level security;
alter table public.pet_perception enable row level security;

drop policy if exists "public insert pet_state" on public.pet_state;
create policy "public insert pet_state"
    on public.pet_state for insert
    to anon with check (true);

drop policy if exists "public select pet_state" on public.pet_state;
create policy "public select pet_state"
    on public.pet_state for select
    to anon using (true);

drop policy if exists "public insert pet_perception" on public.pet_perception;
create policy "public insert pet_perception"
    on public.pet_perception for insert
    to anon with check (true);

drop policy if exists "public select pet_perception" on public.pet_perception;
create policy "public select pet_perception"
    on public.pet_perception for select
    to anon using (true);

-- 4. 示例：往 pet_state 里插入一条推送试试（可注释掉）
-- insert into public.pet_state (payload)
-- values ('{"emotion":"love","bubble":"宝宝最可爱了","ts":"2026-07-31T13:15:00Z"}'::jsonb);
