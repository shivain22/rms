import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';

import { IDatabase } from './database.model';

const initialState = {
  loading: false,
  errorMessage: null,
  databases: [] as ReadonlyArray<IDatabase>,
  database: null as IDatabase | null,
  activeDatabases: [] as ReadonlyArray<IDatabase>,
};

export type DatabaseManagementState = Readonly<typeof initialState>;

// Actions
export const getDatabases = createAsyncThunk('databaseManagement/fetch_all', async () => {
  const requestUrl = 'api/databases';
  return axios.get<IDatabase[]>(requestUrl);
});

export const getDatabase = createAsyncThunk('databaseManagement/fetch_one', async (id: string | number) => {
  const requestUrl = `api/databases/${id}`;
  return axios.get<IDatabase>(requestUrl);
});

export const getDatabasesByVendorId = createAsyncThunk('databaseManagement/fetch_by_vendor', async (vendorId: string | number) => {
  const requestUrl = `api/databases?vendorId=${vendorId}`;
  return axios.get<IDatabase[]>(requestUrl);
});

export const createDatabase = createAsyncThunk('databaseManagement/create', async (database: IDatabase) => {
  const result = await axios.post<IDatabase>('api/databases', database);
  return result;
});

export const updateDatabase = createAsyncThunk('databaseManagement/update', async (database: IDatabase) => {
  const result = await axios.put<IDatabase>(`api/databases/${database.id}`, database);
  return result;
});

export const deleteDatabase = createAsyncThunk('databaseManagement/delete', async (id: string | number) => {
  const requestUrl = `api/databases/${id}`;
  await axios.delete(requestUrl);
  return id;
});

export const DatabaseManagementSlice = createSlice({
  name: 'databaseManagement',
  initialState: initialState as DatabaseManagementState,
  reducers: {
    reset() {
      return initialState;
    },
  },
  extraReducers(builder) {
    builder
      .addCase(getDatabases.pending, state => {
        state.loading = true;
      })
      .addCase(getDatabases.fulfilled, (state, action) => {
        state.loading = false;
        const databases = action.payload.data;
        state.databases = databases;
        state.activeDatabases = databases.filter(d => d.active !== false);
      })
      .addCase(getDatabases.rejected, (state, action) => {
        state.loading = false;
        state.errorMessage = action.error.message;
      })
      .addCase(getDatabase.pending, state => {
        state.loading = true;
      })
      .addCase(getDatabase.fulfilled, (state, action) => {
        state.loading = false;
        state.database = action.payload.data;
      })
      .addCase(getDatabase.rejected, (state, action) => {
        state.loading = false;
        state.errorMessage = action.error.message;
      })
      .addCase(getDatabasesByVendorId.fulfilled, (state, action) => {
        state.loading = false;
        const databases = action.payload.data;
        state.databases = databases;
        state.activeDatabases = databases.filter(d => d.active !== false);
      })
      .addCase(createDatabase.fulfilled, (state, action) => {
        state.loading = false;
        const database = action.payload.data;
        state.databases = [...state.databases, database];
        if (database.active !== false) {
          state.activeDatabases = [...state.activeDatabases, database];
        }
      })
      .addCase(updateDatabase.fulfilled, (state, action) => {
        state.loading = false;
        const database = action.payload.data;
        const index = state.databases.findIndex(d => d.id === database.id);
        if (index >= 0) {
          state.databases[index] = database;
        }
        if (state.database?.id === database.id) {
          state.database = database;
        }
        if (database.active !== false) {
          const activeIndex = state.activeDatabases.findIndex(d => d.id === database.id);
          if (activeIndex >= 0) {
            state.activeDatabases[activeIndex] = database;
          } else {
            state.activeDatabases = [...state.activeDatabases, database];
          }
        } else {
          state.activeDatabases = state.activeDatabases.filter(d => d.id !== database.id);
        }
      })
      .addCase(deleteDatabase.fulfilled, (state, action) => {
        state.loading = false;
        const id = action.payload;
        state.databases = state.databases.filter(d => d.id !== id);
        state.activeDatabases = state.activeDatabases.filter(d => d.id !== id);
        if (state.database?.id === id) {
          state.database = null;
        }
      });
  },
});

export const { reset } = DatabaseManagementSlice.actions;

export default DatabaseManagementSlice.reducer;
